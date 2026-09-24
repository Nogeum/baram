package org.example.portal;

import org.example.portal.domain.*;
import org.example.portal.repository.*;
import org.example.portal.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.time.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class PortalIntegrationTests {
    @Autowired MockMvc mvc;
    @Autowired PortalService service;
    @Autowired OvertimeService overtimeService;
    @Autowired OvertimeRequestRepository overtime;
    @Autowired EmployeeRepository employees;
    @Autowired AttendanceRepository attendance;
    @Autowired AttendanceCorrectionRepository corrections;
    @Autowired LeaveRequestRepository leaves;
    @Autowired ScheduleRepository schedules;
    @Autowired NoticeRepository notices;
    @Autowired NotificationRepository notifications;
    @Autowired org.springframework.security.core.session.SessionRegistry sessionRegistry;
    @MockitoBean Clock clock;
    Employee employee;
    @BeforeEach void setup() {
        corrections.deleteAll(); notifications.deleteAll(); notices.deleteAll(); schedules.deleteAll(); attendance.deleteAll(); leaves.deleteAll(); overtime.deleteAll(); employees.deleteAll();
        at("2026-09-21T09:00:00");
        service.createEmployee("admin","Admin123!","관리자","인사팀","팀장",Employee.Role.ADMIN,"","",LocalDate.of(2020,1,1));
        createManager("manager","개발팀");
        employee=service.createEmployee("employee","Employee123!","김직원","개발팀","사원",Employee.Role.EMPLOYEE,"","",LocalDate.of(2026,1,1));
    }
    private Employee createManager(String login,String department) { return service.createEmployee(login,"Manager123!",login,department,"부장",Employee.Role.EMPLOYEE,"","",service.today(),true); }
    private Long leaveApprover() { return employees.findByLoginId("manager").orElseThrow().getId(); }
    private void at(String time) { when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul")); when(clock.instant()).thenReturn(LocalDateTime.parse(time).atZone(ZoneId.of("Asia/Seoul")).toInstant()); }
    @Test void loginRedirectsByRoleAndPasswordsAreHashed() throws Exception {
        mvc.perform(formLogin().user("admin").password("Admin123!")).andExpect(redirectedUrl("/admin/main"));
        mvc.perform(formLogin().user("employee").password("Employee123!")).andExpect(redirectedUrl("/employee/main"));
        mvc.perform(formLogin().user("employee").password("wrong")).andExpect(redirectedUrl("/login?error"));
        assertThat(employee.getPassword()).startsWith("$2").doesNotContain("Employee123!");
    }
    @Test void accessControlAndCsrfAreEnforced() throws Exception {
        mvc.perform(get("/employee/main")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/admin/main").with(user("employee").roles("EMPLOYEE"))).andExpect(status().isForbidden());
        mvc.perform(post("/employee/check-in").with(user("employee").roles("EMPLOYEE"))).andExpect(status().isForbidden());
        mvc.perform(post("/employee/check-in").with(user("employee").roles("EMPLOYEE")).with(csrf())).andExpect(redirectedUrl("/employee/main"));
        mvc.perform(post("/employee/check-out").with(user("employee").roles("EMPLOYEE")).with(csrf())).andExpect(redirectedUrl("/employee/main"));
    }
    @Test void allPagesRenderWithEmptyAndPopulatedData() throws Exception {
        renderAll();
        service.checkIn("employee");
        service.requestLeave("employee",leaveApprover(),LeaveRequest.Kind.ANNUAL,LocalDate.of(2026,9,22),LocalDate.of(2026,9,23),"개인 사유");
        service.addSchedule("manager",employee.getId(),"팀 회의",service.today(),LocalTime.of(10,0),LocalTime.of(11,0),"업무 공유",Schedule.Color.BLUE);
        service.publish("admin","공지 제목","공지 내용");
        renderAll();
        mvc.perform(get("/manager/main").with(user("manager").roles("EMPLOYEE")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("사내 공지사항"))))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"notification-link\" href=\"/manager/approval\"")));
        mvc.perform(get("/employee/main").with(user("employee").roles("EMPLOYEE")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("사내 공지사항")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("class=\"notification-link\" href=\"/employee/schedule\"")));
    }
    @Test void notificationPopupIsPersonalAndReadRequiresOwnershipAndCsrf() throws Exception {
        var notification = new Notification();
        notification.setRecipient(employee);
        notification.setMessage("개인 알림 확인");
        notification.setCreatedAt(LocalDateTime.now(clock));
        notification = notifications.save(notification);
        mvc.perform(get("/employee/mypage").with(user("employee").roles("EMPLOYEE")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("개인 알림 확인")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("notification-popup")))
            .andExpect(model().attribute("unreadNotifications", 1L));
        mvc.perform(get("/manager/main").with(user("manager").roles("EMPLOYEE")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("개인 알림 확인"))))
            .andExpect(model().attribute("unreadNotifications", 0L));
        String path = "/notifications/" + notification.getId() + "/read";
        mvc.perform(post(path).with(user("employee").roles("EMPLOYEE"))).andExpect(status().isForbidden());
        mvc.perform(post(path).with(user("manager").roles("EMPLOYEE")).with(csrf())).andExpect(status().isBadRequest());
        assertThat(notifications.findById(notification.getId()).orElseThrow().isReadFlag()).isFalse();
        mvc.perform(post(path).with(user("employee").roles("EMPLOYEE")).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(post(path).with(user("employee").roles("EMPLOYEE")).with(csrf())).andExpect(status().isNoContent());
        assertThat(notifications.findById(notification.getId()).orElseThrow().isReadFlag()).isTrue();
        mvc.perform(get("/employee/main").with(user("employee").roles("EMPLOYEE")))
            .andExpect(model().attribute("unreadNotifications", 0L));
    }
    private void renderAll() throws Exception {
        for(String page:new String[]{"main","attendance-manage","schedule-assign","approval"})
            mvc.perform(get("/manager/"+page).with(user("manager").roles("EMPLOYEE"))).andExpect(status().isOk());
        mvc.perform(get("/login")).andExpect(status().isOk());
        mvc.perform(get("/employee/attendance").with(user("employee").roles("EMPLOYEE"))).andExpect(redirectedUrl("/employee/main"));
        for(String page:new String[]{"main","schedule","attendance-status","leave-request","overtime-request","request-status","mypage"})
            mvc.perform(get("/employee/"+page).with(user("employee").roles("EMPLOYEE"))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("김직원")));
        for(String page:new String[]{"main","employee-manage","notification"})
            mvc.perform(get("/admin/"+page).with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
    }
    @Test void exactBoundariesAndDuplicates() {
        assertThatThrownBy(()->service.checkOut("employee")).isInstanceOf(BusinessException.class);
        service.checkIn("employee");
        assertThatThrownBy(()->service.checkIn("employee")).isInstanceOf(BusinessException.class);
        at("2026-09-21T18:00:00"); service.checkOut("employee");
        var a=attendance.findAll().get(0); assertThat(a.isLateArrival()).isFalse(); assertThat(a.isEarlyDeparture()).isFalse();
        assertThatThrownBy(()->service.checkOut("employee")).isInstanceOf(BusinessException.class);
    }
    @Test void lateAndEarlyAreBothPreserved() {
        at("2026-09-21T09:00:01"); service.checkIn("employee");
        at("2026-09-21T17:59:59"); service.checkOut("employee");
        var a=attendance.findAll().get(0); assertThat(a.isLateArrival()).isTrue(); assertThat(a.isEarlyDeparture()).isTrue();
    }
    @Test void leaveReservationReviewAndCancellation() {
        var start=LocalDate.of(2026,9,25); var end=LocalDate.of(2026,9,28);
        service.requestLeave("employee",leaveApprover(),LeaveRequest.Kind.ANNUAL,start,end,"휴식");
        var r=leaves.findAll().get(0); assertThat(r.getDays()).isEqualTo(2); assertThat(service.remaining(employee,2026)).isEqualTo(13);
        assertThatThrownBy(()->service.requestLeave("employee",leaveApprover(),LeaveRequest.Kind.ANNUAL,start,end,"중복")).isInstanceOf(BusinessException.class);
        service.review("manager",r.getId(),true,""); assertThat(service.remaining(employee,2026)).isEqualTo(13);
        assertThatThrownBy(()->service.review("manager",r.getId(),false,"반려")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->service.cancelLeave("employee",r.getId())).isInstanceOf(BusinessException.class);
        service.requestLeave("employee",leaveApprover(),LeaveRequest.Kind.HALF_AM,LocalDate.of(2026,9,29),LocalDate.of(2026,9,29),"오전");
        var half=leaves.findByStatusOrderByRequestedAtAsc(LeaveRequest.Status.PENDING).get(0);
        assertThat(service.remaining(employee,2026)).isEqualTo(12.5);
        service.cancelLeave("employee",half.getId()); assertThat(service.remaining(employee,2026)).isEqualTo(13);
    }
    @Test void leaveValidationAndRejectionReleaseReservation() {
        var date=LocalDate.of(2026,9,22);
        assertThatThrownBy(()->service.requestLeave("employee",leaveApprover(),LeaveRequest.Kind.ANNUAL,date,LocalDate.of(2026,12,31),"초과")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->service.requestLeave("employee",leaveApprover(),LeaveRequest.Kind.HALF_AM,date,date.plusDays(1),"반차")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->service.requestLeave("employee",leaveApprover(),LeaveRequest.Kind.ANNUAL,LocalDate.of(2026,9,26),LocalDate.of(2026,9,27),"주말")).isInstanceOf(BusinessException.class);
        service.requestLeave("employee",leaveApprover(),LeaveRequest.Kind.ANNUAL,date,date,"신청"); var r=leaves.findAll().get(0);
        assertThatThrownBy(()->service.review("manager",r.getId(),false,"")).isInstanceOf(BusinessException.class);
        service.review("manager",r.getId(),false,"일정 조정 필요"); assertThat(service.remaining(employee,2026)).isEqualTo(15);
    }
    @Test void ownershipAndInactiveAccounts() throws Exception {
        service.createEmployee("other","Other123!","다른직원","QA","사원",Employee.Role.EMPLOYEE,"","",service.today());
        service.requestLeave("employee",leaveApprover(),LeaveRequest.Kind.ANNUAL,service.today(),service.today(),"개인");
        var r=leaves.findAll().get(0);
        assertThatThrownBy(()->service.cancelLeave("other",r.getId())).isInstanceOf(BusinessException.class);
        service.addSchedule("manager",employee.getId(),"배정",service.today(),null,null,"",Schedule.Color.BLUE);
        assertThatThrownBy(()->service.deleteSchedule("employee",schedules.findAll().get(0).getId())).isInstanceOf(BusinessException.class);
        service.updateEmployee("admin",employee.getId(),"김직원","개발팀","사원",false,15);
        mvc.perform(formLogin().user("employee").password("Employee123!")).andExpect(redirectedUrl("/login?error"));
        assertThatThrownBy(()->service.checkIn("employee")).isInstanceOf(BusinessException.class);
    }
    @Test void concurrentCheckInCreatesOneRecord() throws Exception {
        var executor=Executors.newFixedThreadPool(2); var start=new CountDownLatch(1);
        Callable<Boolean> action=()->{start.await(); try{service.checkIn("employee"); return true;}catch(BusinessException e){return false;}};
        try { var first=executor.submit(action); var second=executor.submit(action); start.countDown();
            assertThat(java.util.List.of(first.get(10,TimeUnit.SECONDS),second.get(10,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
            assertThat(attendance.count()).isEqualTo(1);
        } finally {executor.shutdownNow();}
    }
    @Test void calendarPersistsColorAndKeepsMonthAndOwnership() throws Exception {
        mvc.perform(post("/employee/schedule").with(user("employee").roles("EMPLOYEE")).with(csrf())
            .param("title","윤년 개인 일정").param("eventDate","2028-02-29").param("memo","메모 <script>alert(1)</script>").param("color","PURPLE"))
            .andExpect(redirectedUrl("/employee/schedule?date=2028-02-29"));
        var own=schedules.findAll().get(0);
        assertThat(own.getColor()).isEqualTo(Schedule.Color.PURPLE);
        service.createEmployee("other","Other123!","다른직원","QA","사원",Employee.Role.EMPLOYEE,"","",service.today());
        service.addSchedule("other",null,"다른 직원의 비공개 일정",LocalDate.of(2028,2,29),null,null,"",Schedule.Color.RED);
        var result=mvc.perform(get("/employee/schedule").param("date","2028-02-29").with(user("employee").roles("EMPLOYEE")))
            .andExpect(status().isOk()).andReturn();
        var html=result.getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(html).contains("윤년 개인 일정","--event-color: #8e44ad").doesNotContain("다른 직원의 비공개 일정","<script>alert(1)</script>");
        @SuppressWarnings("unchecked")
        var days=(java.util.List<org.example.portal.web.CalendarDay>)result.getModelAndView().getModel().get("calendarDays");
        assertThat(days).hasSize(35);
        assertThat(days.stream().filter(org.example.portal.web.CalendarDay::currentMonth).count()).isEqualTo(29);
        assertThat(days.get(0).date().getDayOfWeek()).isEqualTo(DayOfWeek.SUNDAY);
        mvc.perform(post("/employee/schedule").with(user("employee").roles("EMPLOYEE")).with(csrf())
            .param("title","잘못된 색상").param("eventDate","2028-02-29").param("color","INVALID"))
            .andExpect(status().isBadRequest());
        assertThat(schedules.count()).isEqualTo(2);
        mvc.perform(post("/employee/schedule/"+own.getId()+"/delete").with(user("employee").roles("EMPLOYEE")).with(csrf()))
            .andExpect(redirectedUrl("/employee/schedule?date=2028-02-29"));
        assertThat(schedules.existsById(own.getId())).isFalse();
    }
    @Test void personalScheduleEditsPersistAndEnforceOwnership() throws Exception {
        service.addSchedule("employee",null,"원래 일정",service.today(),null,null,"",Schedule.Color.BLUE);
        var id=schedules.findAll().get(0).getId();
        mvc.perform(post("/employee/schedule/"+id+"/edit").with(user("employee").roles("EMPLOYEE"))
            .param("title","변경").param("eventDate","2026-10-02").param("color","TEAL")).andExpect(status().isForbidden());
        mvc.perform(post("/employee/schedule/"+id+"/edit").with(user("employee").roles("EMPLOYEE")).with(csrf())
            .param("title","변경 일정").param("eventDate","2026-10-02").param("startTime","10:00").param("endTime","11:30").param("memo","변경 메모").param("color","TEAL"))
            .andExpect(redirectedUrl("/employee/schedule?date=2026-10-02"));
        var updated=schedules.findById(id).orElseThrow();
        assertThat(updated.getTitle()).isEqualTo("변경 일정"); assertThat(updated.getColor()).isEqualTo(Schedule.Color.TEAL);
        assertThat(updated.getEventDate()).isEqualTo(LocalDate.of(2026,10,2)); assertThat(updated.getStartTime()).isEqualTo(LocalTime.of(10,0));
        assertThat(updated.getEndTime()).isEqualTo(LocalTime.of(11,30)); assertThat(updated.getMemo()).isEqualTo("변경 메모");
        assertThat(schedules.count()).isEqualTo(1);
        service.createEmployee("other","Other123!","다른직원","QA","사원",Employee.Role.EMPLOYEE,"","",service.today());
        assertThatThrownBy(()->service.updateSchedule("other",id,"변경 불가",service.today(),null,null,"",Schedule.Color.RED)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->service.updateSchedule("employee",id,"잘못된 시간",service.today(),LocalTime.NOON,LocalTime.of(9,0),"",Schedule.Color.RED)).isInstanceOf(BusinessException.class);
        assertThat(schedules.findById(id).orElseThrow().getTitle()).isEqualTo("변경 일정");
        service.addSchedule("manager",employee.getId(),"배정 일정",service.today(),null,null,"",Schedule.Color.BLUE);
        var assigned=schedules.findAll().stream().filter(Schedule::isAssigned).findFirst().orElseThrow();
        assertThatThrownBy(()->service.updateSchedule("employee",assigned.getId(),"변경 불가",service.today(),null,null,"",Schedule.Color.RED)).isInstanceOf(BusinessException.class);
        service.updateSchedule("employee",id,"종일로 변경",service.today(),null,null,"",Schedule.Color.PURPLE);
        assertThat(schedules.findById(id).orElseThrow().getStartTime()).isNull();
        assertThat(schedules.findById(id).orElseThrow().getEndTime()).isNull();
    }
    @Test void overtimeRequestApprovalOwnershipAndCancellation() throws Exception {
        at("2026-09-21T22:00:00");
        var admin=employees.findByLoginId("manager").orElseThrow();
        mvc.perform(post("/employee/overtime").with(user("employee").roles("EMPLOYEE"))
            .param("approverId",admin.getId().toString()).param("workDate","2026-09-21").param("startTime","18:00").param("endTime","20:30").param("reason","배포 준비"))
            .andExpect(status().isForbidden());
        mvc.perform(post("/employee/overtime").with(user("employee").roles("EMPLOYEE")).with(csrf())
            .param("approverId",admin.getId().toString()).param("workDate","2026-09-21").param("startTime","18:00").param("endTime","20:30").param("reason","배포 준비"))
            .andExpect(redirectedUrl("/employee/request-status"));
        var request=overtime.findAll().get(0); assertThat(request.getMinutes()).isEqualTo(150);
        mvc.perform(get("/employee/request-status").with(user("employee").roles("EMPLOYEE"))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("배포 준비")));
        mvc.perform(get("/manager/approval").with(user("manager").roles("EMPLOYEE"))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("배포 준비")));
        mvc.perform(get("/employee/main").with(user("employee").roles("EMPLOYEE"))).andExpect(model().attribute("pending",1L));
        service.createEmployee("other","Other123!","다른직원","QA","사원",Employee.Role.EMPLOYEE,"","",service.today());
        createManager("admin2","QA");
        mvc.perform(get("/employee/request-status").with(user("other").roles("EMPLOYEE"))).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("배포 준비"))));
        mvc.perform(get("/manager/approval").with(user("admin2").roles("EMPLOYEE"))).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("배포 준비"))));
        assertThatThrownBy(()->overtimeService.cancel("other",request.getId())).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->overtimeService.review("admin2",request.getId(),true,"")).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(()->overtimeService.review("manager",request.getId(),false,"")).isInstanceOf(BusinessException.class);
        mvc.perform(post("/manager/overtime/"+request.getId()+"/review").with(user("manager").roles("EMPLOYEE")).with(csrf()).param("approve","true"))
            .andExpect(redirectedUrl("/manager/approval"));
        assertThat(overtime.findById(request.getId()).orElseThrow().getStatus()).isEqualTo(OvertimeRequest.Status.APPROVED);
        assertThat(attendance.count()).isZero(); assertThat(service.remaining(employee,2026)).isEqualTo(15);
        assertThatThrownBy(()->overtimeService.cancel("employee",request.getId())).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->overtimeService.review("manager",request.getId(),true,"")).isInstanceOf(BusinessException.class);
        overtimeService.request("employee",admin.getId(),service.today(),LocalTime.of(21,0),LocalTime.of(22,0),"추가 작업");
        var second=overtime.findAll().stream().filter(r->r.getStatus()==OvertimeRequest.Status.PENDING).findFirst().orElseThrow();
        overtimeService.cancel("employee",second.getId());
        assertThat(overtime.findById(second.getId()).orElseThrow().getStatus()).isEqualTo(OvertimeRequest.Status.CANCELLED);
        overtimeService.request("employee",admin.getId(),service.today(),LocalTime.of(21,0),LocalTime.of(22,0),"다시 신청");
        var third=overtime.findAll().stream().filter(r->r.getStatus()==OvertimeRequest.Status.PENDING).findFirst().orElseThrow();
        overtimeService.review("manager",third.getId(),false,"일정 조정 필요");
        assertThat(overtime.findById(third.getId()).orElseThrow().getReviewComment()).isEqualTo("일정 조정 필요");
    }
    @Test void overtimeOnlyAcceptsCompletedWorkSinceHireDate() throws Exception {
        var approver=leaveApprover();
        var today=service.today();
        overtimeService.request("employee",approver,today.minusDays(1),LocalTime.of(18,0),LocalTime.of(20,0),"지난 근무");
        overtimeService.request("employee",approver,employee.getHireDate(),LocalTime.of(18,0),LocalTime.of(20,0),"입사일 근무");
        overtimeService.request("employee",approver,today,LocalTime.of(8,0),LocalTime.of(9,0),"방금 종료");
        assertThatThrownBy(()->overtimeService.request("employee",approver,today,LocalTime.of(9,0),LocalTime.of(9,1),"진행 중"))
            .isInstanceOf(BusinessException.class).hasMessageContaining("이미 종료된");
        assertThatThrownBy(()->overtimeService.request("employee",approver,employee.getHireDate().minusDays(1),LocalTime.of(18,0),LocalTime.of(20,0),"입사 전"))
            .isInstanceOf(BusinessException.class).hasMessageContaining("입사일");
        mvc.perform(post("/employee/overtime").with(user("employee").roles("EMPLOYEE")).with(csrf())
            .param("approverId",approver.toString()).param("workDate",today.plusDays(1).toString())
            .param("startTime","18:00").param("endTime","20:00").param("reason","미래 근무"))
            .andExpect(status().isBadRequest());
        mvc.perform(get("/employee/overtime-request").with(user("employee").roles("EMPLOYEE")))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("max=\"2026-09-21\"")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("min=\"2026-01-01\"")));
        assertThat(overtime.count()).isEqualTo(3);
    }
    @Test void overtimeValidatesTimeApproverAndOverlap() {
        at("2026-09-21T22:00:00");
        var admin=employees.findByLoginId("manager").orElseThrow().getId(); var date=service.today();
        assertThatThrownBy(()->overtimeService.request("employee",admin,date.plusDays(1),LocalTime.of(18,0),LocalTime.of(20,0),"사유")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->overtimeService.request("employee",admin,date,LocalTime.of(20,0),LocalTime.of(18,0),"사유")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->overtimeService.request("employee",employee.getId(),date,LocalTime.of(18,0),LocalTime.of(20,0),"사유")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->overtimeService.request("employee",admin,date,LocalTime.of(18,0),LocalTime.of(20,0)," ")).isInstanceOf(BusinessException.class);
        overtimeService.request("employee",admin,date,LocalTime.of(18,0),LocalTime.of(20,0),"사유");
        assertThatThrownBy(()->overtimeService.request("employee",admin,date,LocalTime.of(19,0),LocalTime.of(21,0),"중복")).isInstanceOf(BusinessException.class);
        overtimeService.request("employee",admin,date,LocalTime.of(20,0),LocalTime.of(21,0),"연속 근무");
        assertThat(overtime.count()).isEqualTo(2);
    }
    @Test void leaveApproverIsRequiredAndControlsInboxAndReview() throws Exception {
        var second=createManager("admin2","개발팀");
        mvc.perform(post("/employee/leave").with(user("employee").roles("EMPLOYEE")).with(csrf())
            .param("kind","ANNUAL").param("startDate","2026-09-22").param("endDate","2026-09-22").param("reason","누락"))
            .andExpect(status().isBadRequest());
        assertThatThrownBy(()->service.requestLeave("employee",employee.getId(),LeaveRequest.Kind.ANNUAL,service.today(),service.today(),"사유")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->service.requestLeave("employee",null,LeaveRequest.Kind.ANNUAL,service.today(),service.today(),"사유")).isInstanceOf(BusinessException.class);
        second.setActive(false); employees.save(second);
        assertThatThrownBy(()->service.requestLeave("employee",second.getId(),LeaveRequest.Kind.ANNUAL,service.today(),service.today(),"사유")).isInstanceOf(BusinessException.class);
        var enabled=employees.findById(second.getId()).orElseThrow(); enabled.setActive(true); employees.save(enabled);
        mvc.perform(post("/employee/leave").with(user("employee").roles("EMPLOYEE")).with(csrf())
            .param("approverId",second.getId().toString()).param("kind","ANNUAL").param("startDate","2026-09-22").param("endDate","2026-09-22").param("reason","지정 결재 테스트"))
            .andExpect(redirectedUrl("/employee/request-status"));
        var request=leaves.findAll().get(0);
        assertThat(request.getApprover().getId()).isEqualTo(second.getId());
        assertThat(notifications.findTop100ByRecipientIdOrderByCreatedAtDesc(leaveApprover())).isEmpty();
        assertThat(notifications.findTop100ByRecipientIdOrderByCreatedAtDesc(second.getId())).hasSize(1);
        mvc.perform(get("/manager/approval").with(user("manager").roles("EMPLOYEE"))).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("지정 결재 테스트"))));
        mvc.perform(get("/manager/approval").with(user("admin2").roles("EMPLOYEE"))).andExpect(content().string(org.hamcrest.Matchers.containsString("지정 결재 테스트")));
        assertThatThrownBy(()->service.review("manager",request.getId(),true,"")).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        service.review("admin2",request.getId(),true,"");
        assertThat(leaves.findById(request.getId()).orElseThrow().getReviewer().getId()).isEqualTo(second.getId());
        // Pre-migration requests retain the original shared inbox behavior.
        service.requestLeave("employee",leaveApprover(),LeaveRequest.Kind.ANNUAL,LocalDate.of(2026,9,23),LocalDate.of(2026,9,23),"기존 신청");
        var legacy=leaves.findByStatusOrderByRequestedAtAsc(LeaveRequest.Status.PENDING).get(0); legacy.setApprover(null); leaves.save(legacy);
        assertThat(leaves.pendingForApprover(second.getId(),LeaveRequest.Status.PENDING)).hasSize(1);
        service.review("admin2",legacy.getId(),false,"반려 사유");
        assertThat(leaves.findById(legacy.getId()).orElseThrow().getStatus()).isEqualTo(LeaveRequest.Status.REJECTED);
    }
    @Test void departmentScopeAndRevocationAreEnforced() throws Exception {
        var other=service.createEmployee("outside","Outside123!","외부직원","영업팀","부장",Employee.Role.EMPLOYEE,"","",service.today());
        var otherManager=createManager("salesboss","영업팀");
        var manager=employees.findByLoginId("manager").orElseThrow();
        service.checkIn("employee"); service.checkIn("outside");
        service.addSchedule("salesboss",other.getId(),"다른 부서 배정",service.today(),null,null,"",Schedule.Color.BLUE);
        service.addSchedule("employee",null,"개인 비공개 일정",service.today(),null,null,"",Schedule.Color.BLUE);
        mvc.perform(get("/manager/attendance-manage").with(user("manager").roles("EMPLOYEE"))).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("김직원")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("외부직원"))));
        mvc.perform(get("/manager/schedule-assign").with(user("manager").roles("EMPLOYEE"))).andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("다른 부서 배정"))))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("개인 비공개 일정"))));
        for(String login:new String[]{"employee","outside"}) mvc.perform(get("/manager/main").with(user(login).roles("EMPLOYEE"))).andExpect(status().isForbidden());
        mvc.perform(get("/manager/main").with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(get("/admin/approval").with(user("admin").roles("ADMIN"))).andExpect(status().isNotFound());
        mvc.perform(post("/manager/schedule").with(user("manager").roles("EMPLOYEE")).with(csrf())
            .param("employeeId",other.getId().toString()).param("title","외부 배정 시도").param("eventDate","2026-09-21")).andExpect(status().isForbidden());
        var outsideSchedule=schedules.findAll().stream().filter(Schedule::isAssigned).findFirst().orElseThrow();
        mvc.perform(post("/manager/schedule/"+outsideSchedule.getId()+"/delete").with(user("manager").roles("EMPLOYEE")).with(csrf())).andExpect(status().isForbidden());
        assertThatThrownBy(()->service.requestLeave("employee",otherManager.getId(),LeaveRequest.Kind.ANNUAL,service.today(),service.today(),"외부 결재자")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->service.requestLeave("manager",manager.getId(),LeaveRequest.Kind.ANNUAL,service.today(),service.today(),"자기 결재")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->overtimeService.request("manager",manager.getId(),service.today(),LocalTime.of(7,0),LocalTime.of(8,0),"자기 결재")).isInstanceOf(BusinessException.class);
        service.updateEmployee("admin",manager.getId(),manager.getName(),manager.getDepartment(),manager.getPositionName(),true,15,false);
        mvc.perform(get("/manager/main").with(user("manager").roles("EMPLOYEE"))).andExpect(status().isForbidden());
        mvc.perform(get("/employee/main").with(user("manager").roles("EMPLOYEE"))).andExpect(status().isOk());
    }
    @Test void departmentChangesAndLegacyApproversStayWithinCurrentDepartment() {
        at("2026-09-21T22:00:00");
        var manager=employees.findByLoginId("manager").orElseThrow();
        var sales=createManager("salesboss","영업팀");
        service.requestLeave("employee",manager.getId(),LeaveRequest.Kind.ANNUAL,service.today(),service.today(),"이동 전 신청");
        overtimeService.request("employee",manager.getId(),service.today(),LocalTime.of(18,0),LocalTime.of(20,0),"이동 전 초과근무");
        var request=leaves.findAll().get(0); var ot=overtime.findAll().get(0);
        service.updateEmployee("admin",employee.getId(),employee.getName(),"영업팀",employee.getPositionName(),true,15);
        assertThatThrownBy(()->service.review("manager",request.getId(),true,"")).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(()->overtimeService.review("manager",ot.getId(),true,"")).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        service.review("salesboss",request.getId(),true,""); overtimeService.review("salesboss",ot.getId(),true,"");
        assertThat(leaves.findById(request.getId()).orElseThrow().getApprover().getId()).isEqualTo(sales.getId());
        assertThat(overtime.findById(ot.getId()).orElseThrow().getApprover().getId()).isEqualTo(sales.getId());
    }
    @Test void accountDeletionRequiresAdminConfirmationAndCsrf() throws Exception {
        var target=service.createEmployee("delete_test","Delete123!","삭제 테스트","개발팀","사원",Employee.Role.EMPLOYEE,"","",service.today());
        String url="/admin/employees/"+target.getId()+"/delete";
        mvc.perform(post(url).with(user("employee").roles("EMPLOYEE")).with(csrf()).param("confirmLoginId","delete_test")).andExpect(status().isForbidden());
        mvc.perform(post(url).with(user("admin").roles("ADMIN")).param("confirmLoginId","delete_test")).andExpect(status().isForbidden());
        assertThatThrownBy(()->service.deleteEmployee("manager",target.getId(),"delete_test")).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        mvc.perform(post(url).with(user("admin").roles("ADMIN")).with(csrf()).param("confirmLoginId","wrong"))
            .andExpect(redirectedUrl("/admin/employee-manage")).andExpect(flash().attributeExists("error"));
        assertThat(employees.existsById(target.getId())).isTrue();
        mvc.perform(post(url).with(user("admin").roles("ADMIN")).with(csrf()).param("confirmLoginId","delete_test"))
            .andExpect(redirectedUrl("/admin/employee-manage")).andExpect(flash().attributeExists("success"));
        var deleted=employees.findById(target.getId()).orElseThrow();
        assertThat(deleted.isDeleted()).isTrue();
        assertThat(deleted.isActive()).isFalse();
        assertThat(deleted.getDeletedByLogin()).isEqualTo("admin");
        assertThat(employees.findByDeletedAtIsNullOrderByNameAsc()).extracting(Employee::getId).doesNotContain(target.getId());
        assertThatThrownBy(()->service.updateEmployee("admin",target.getId(),"name","dept","staff",true,15)).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->service.createEmployee("delete_test","Delete123!","name","dept","staff",Employee.Role.EMPLOYEE,"","",service.today())).isInstanceOf(BusinessException.class);
        mvc.perform(formLogin().user("delete_test").password("Delete123!")).andExpect(redirectedUrl("/login?error"));
        var admin=employees.findByLoginId("admin").orElseThrow();
        assertThatThrownBy(()->service.deleteEmployee("admin",admin.getId(),"admin")).isInstanceOf(BusinessException.class);
        assertThat(employees.existsById(admin.getId())).isTrue();
    }
    @Test void deletingAccountExpiresExistingSessions() throws Exception {
        var target=service.createEmployee("temporary_admin","Delete123!","임시 관리자","인사","관리자",Employee.Role.ADMIN,"","",service.today());
        var login=mvc.perform(formLogin().user("temporary_admin").password("Delete123!")).andExpect(redirectedUrl("/admin/main")).andReturn();
        var session=(org.springframework.mock.web.MockHttpSession)login.getRequest().getSession(false);
        assertThat(sessionRegistry.getSessionInformation(session.getId()).isExpired()).isFalse();
        mvc.perform(post("/admin/employees/"+target.getId()+"/delete").with(user("admin").roles("ADMIN")).with(csrf()).param("confirmLoginId","temporary_admin"))
            .andExpect(redirectedUrl("/admin/employee-manage"));
        assertThat(sessionRegistry.getSessionInformation(session.getId()).isExpired()).isTrue();
        mvc.perform(get("/admin/employee-manage").session(session)).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("신규 직원 등록"))));
    }
    private LeaveRequest deletionLeave(Employee applicant,Employee approver,Employee reviewer) {
        var r=new LeaveRequest(); r.setEmployee(applicant); r.setApprover(approver); r.setReviewer(reviewer); r.setKind(LeaveRequest.Kind.ANNUAL);
        r.setStartDate(service.today()); r.setEndDate(service.today()); r.setChargedUnits(2); r.setReason("보존 대상"); r.setRequestedAt(service.today().atStartOfDay()); return r;
    }
    private OvertimeRequest deletionOvertime(Employee applicant,Employee approver) {
        var r=new OvertimeRequest(); r.setEmployee(applicant); r.setApprover(approver); r.setWorkDate(service.today());
        r.setStartTime(LocalTime.of(18,0)); r.setEndTime(LocalTime.of(20,0)); r.setReason("보존 대상"); r.setRequestedAt(service.today().atStartOfDay()); return r;
    }
    @Test void everyLinkedRecordSurvivesAccountDeletion() {
        java.util.List<java.util.function.Consumer<Employee>> seeds=java.util.List.of(
            e->{ var a=new Attendance(); a.setEmployee(e); a.setWorkDate(service.today()); a.setCheckIn(service.today().atTime(9,0)); attendance.save(a); },
            e->leaves.save(deletionLeave(e,null,null)),
            e->leaves.save(deletionLeave(employee,e,null)),
            e->leaves.save(deletionLeave(employee,null,e)),
            e->overtime.save(deletionOvertime(e,employee)),
            e->overtime.save(deletionOvertime(employee,e)),
            e->service.addSchedule(e.getLoginId(),null,"보존 일정",service.today(),null,null,"",Schedule.Color.BLUE),
            e->{ var n=new Notice(); n.setAuthor(e); n.setTitle("보존 공지"); n.setContent("내용"); n.setCreatedAt(service.today().atStartOfDay()); notices.save(n); },
            e->{ var n=new Notification(); n.setRecipient(e); n.setMessage("보존 알림"); n.setCreatedAt(service.today().atStartOfDay()); notifications.save(n); }
        );
        for(int i=0;i<seeds.size();i++) {
            var e=service.createEmployee("linked"+i,"Delete123!","연결 계정","개발팀","사원",Employee.Role.EMPLOYEE,"","",service.today());
            seeds.get(i).accept(e);
            assertThat(service.deleteEmployee("admin",e.getId(),e.getLoginId())).isEqualTo(e.getLoginId());
            var deleted=employees.findById(e.getId()).orElseThrow();
            assertThat(deleted.isDeleted()).isTrue();
            assertThat(deleted.isActive()).isFalse();
            assertThat(deleted.isDepartmentManager()).isFalse();
        }
        assertThat(attendance.count()).isEqualTo(1); assertThat(leaves.count()).isEqualTo(3); assertThat(overtime.count()).isEqualTo(2);
        assertThat(schedules.count()).isEqualTo(1); assertThat(notices.count()).isEqualTo(1); assertThat(notifications.count()).isEqualTo(1);
        assertThat(leaves.findAll()).filteredOn(r->r.getStatus()==LeaveRequest.Status.CANCELLED).hasSize(1);
        assertThat(overtime.findAll()).filteredOn(r->r.getStatus()==OvertimeRequest.Status.CANCELLED).hasSize(1);
    }
    @Test void deletedApproverRequestsTransferToRemainingDepartmentManager() {
        at("2026-09-21T22:00:00");
        var manager=employees.findByLoginId("manager").orElseThrow();
        var replacement=createManager("replacement","개발팀");
        service.requestLeave("employee",manager.getId(),LeaveRequest.Kind.ANNUAL,service.today(),service.today(),"휴가");
        overtimeService.request("employee",manager.getId(),service.today(),LocalTime.of(18,0),LocalTime.of(20,0),"초과근무");
        var leave=leaves.findAll().get(0); var ot=overtime.findAll().get(0);
        service.deleteEmployee("admin",manager.getId(),"manager");
        assertThatThrownBy(()->service.review("manager",leave.getId(),true,"")).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        service.review("replacement",leave.getId(),true,""); overtimeService.review("replacement",ot.getId(),true,"");
        assertThat(leaves.findById(leave.getId()).orElseThrow().getApprover().getId()).isEqualTo(replacement.getId());
        assertThat(overtime.findById(ot.getId()).orElseThrow().getApprover().getId()).isEqualTo(replacement.getId());
        service.deleteEmployee("admin",employee.getId(),"employee");
        assertThat(leaves.findById(leave.getId()).orElseThrow().getStatus()).isEqualTo(LeaveRequest.Status.APPROVED);
        assertThat(overtime.findById(ot.getId()).orElseThrow().getStatus()).isEqualTo(OvertimeRequest.Status.APPROVED);
    }
    @Test void deletedAdminNoticeKeepsAuthorAndAccountDisappearsFromManagement() throws Exception {
        var target=service.createEmployee("mistaken_admin","Delete123!","잘못 만든 관리자","인사","관리자",Employee.Role.ADMIN,"","",service.today());
        service.publish("mistaken_admin","기록 보존 확인","공지 본문");
        service.deleteEmployee("admin",target.getId(),"mistaken_admin");
        mvc.perform(get("/admin/notification").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("잘못 만든 관리자 (삭제된 계정)")));
        mvc.perform(get("/admin/employee-manage").with(user("admin").roles("ADMIN")))
            .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("data-login=\"mistaken_admin\""))));
        assertThat(notices.count()).isEqualTo(1);
    }
    @Test void concurrentAdminDeletionsKeepAnActiveAdministrator() throws Exception {
        var admin=employees.findByLoginId("admin").orElseThrow();
        var other=service.createEmployee("delete_admin","Delete123!","임시 관리자","인사","관리자",Employee.Role.ADMIN,"","",service.today());
        var executor=Executors.newFixedThreadPool(2); var start=new CountDownLatch(1);
        try {
            var one=executor.submit(()->{start.await();try{service.deleteEmployee("admin",other.getId(),"delete_admin");return true;}catch(BusinessException e){return false;}});
            var two=executor.submit(()->{start.await();try{service.deleteEmployee("delete_admin",admin.getId(),"admin");return true;}catch(BusinessException e){return false;}});
            start.countDown(); assertThat(java.util.List.of(one.get(10,TimeUnit.SECONDS),two.get(10,TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
            assertThat(employees.findByRoleAndActiveTrue(Employee.Role.ADMIN)).hasSize(1);
        }finally{executor.shutdownNow();}
    }
    @Test void profilePasswordRequiresCurrentPassword() {
        assertThatThrownBy(()->service.updateProfile("employee","test@example.com","010-1234-5678","wrong","NewPass123!")).isInstanceOf(BusinessException.class);
        service.updateProfile("employee","test@example.com","010-1234-5678","Employee123!","NewPass123!");
        assertThat(employees.findByLoginId("employee").orElseThrow().getEmail()).isEqualTo("test@example.com");
    }
    @Test void formsPersistAndApprovalNotifiesEmployee() throws Exception {
        mvc.perform(post("/employee/leave").with(user("employee").roles("EMPLOYEE")).with(csrf())
            .param("approverId",leaveApprover().toString()).param("kind","ANNUAL").param("startDate","2026-09-22").param("endDate","2026-09-22").param("reason","휴식"))
            .andExpect(redirectedUrl("/employee/request-status"));
        var request=leaves.findAll().get(0);
        mvc.perform(post("/manager/leave/"+request.getId()+"/review").with(user("manager").roles("EMPLOYEE")).with(csrf()).param("approve","true").param("comment","확인"))
            .andExpect(redirectedUrl("/manager/approval"));
        assertThat(leaves.findById(request.getId()).orElseThrow().getStatus()).isEqualTo(LeaveRequest.Status.APPROVED);
        var alert=notifications.findTop100ByRecipientIdOrderByCreatedAtDesc(employee.getId()).get(0);
        mvc.perform(post("/employee/notifications/"+alert.getId()+"/read").with(user("employee").roles("EMPLOYEE")).with(csrf())).andExpect(status().is3xxRedirection());
        assertThat(notifications.findById(alert.getId()).orElseThrow().isReadFlag()).isTrue();
        mvc.perform(post("/manager/schedule").with(user("manager").roles("EMPLOYEE")).with(csrf())
            .param("employeeId",employee.getId().toString()).param("title","팀 회의").param("eventDate","2026-09-23").param("startTime","10:00").param("endTime","11:00"))
            .andExpect(redirectedUrl("/manager/schedule-assign?date=2026-09-23"));
        assertThat(schedules.findAll().get(0).isAssigned()).isTrue();
        mvc.perform(post("/admin/notices").with(user("admin").roles("ADMIN")).with(csrf()).param("title","공지").param("content","안내"))
            .andExpect(redirectedUrl("/admin/notification"));
        assertThat(notices.count()).isEqualTo(1);
        mvc.perform(post("/employee/leave").with(user("employee").roles("EMPLOYEE")).with(csrf())
            .param("approverId",leaveApprover().toString()).param("kind","ANNUAL").param("startDate","invalid").param("endDate","2026-09-22").param("reason","입력 오류"))
            .andExpect(status().isBadRequest());
    }
}
