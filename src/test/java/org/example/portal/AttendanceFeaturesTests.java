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
import java.util.*;
import java.io.*;
import java.util.zip.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class AttendanceFeaturesTests {
    @Autowired MockMvc mvc;
    @Autowired PortalService portal;
    @Autowired AttendanceCorrectionService service;
    @Autowired DepartmentReportService reports;
    @Autowired EmployeeRepository employees;
    @Autowired AttendanceRepository attendance;
    @Autowired AttendanceCorrectionRepository corrections;
    @Autowired LeaveRequestRepository leaves;
    @Autowired OvertimeRequestRepository overtime;
    @Autowired NotificationRepository notifications;
    @Autowired NoticeRepository notices;
    @Autowired ScheduleRepository schedules;
    @MockitoBean Clock clock;
    Employee staff,boss,outsider;
    final LocalDate date=LocalDate.of(2026,9,21);
    @BeforeEach void setup(){
        corrections.deleteAll();notifications.deleteAll();notices.deleteAll();schedules.deleteAll();attendance.deleteAll();leaves.deleteAll();overtime.deleteAll();employees.deleteAll();
        when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));when(clock.instant()).thenReturn(Instant.parse("2026-09-22T10:00:00Z"));
        portal.createEmployee("admin","Admin123!","관리자","인사","관리자",Employee.Role.ADMIN,"","",date);
        boss=portal.createEmployee("boss","Manager123!","개발부장","개발","부장",Employee.Role.EMPLOYEE,"","",date,true);
        outsider=portal.createEmployee("outside","Manager123!","외부부장","영업","부장",Employee.Role.EMPLOYEE,"","",date,true);
        staff=portal.createEmployee("staff","Employee123!","직원","개발","사원",Employee.Role.EMPLOYEE,"","",date);
    }
    AttendanceCorrection request(){service.request("staff",boss.getId(),date,LocalTime.of(9,0),LocalTime.of(18,0),"퇴근 누락");return corrections.findAll().get(0);}
    Attendance record(LocalDate day,String in,String out){var a=new Attendance();a.setEmployee(staff);a.setWorkDate(day);a.setCheckIn(day.atTime(LocalTime.parse(in)));if(out!=null)a.setCheckOut(day.atTime(LocalTime.parse(out)));a.setLateArrival(a.getCheckIn().toLocalTime().isAfter(LocalTime.of(9,0)));a.setEarlyDeparture(out!=null && a.getCheckOut().toLocalTime().isBefore(LocalTime.of(18,0)));return attendance.save(a);}
    @Test void approvalUpdatesAttendanceAndKeepsOriginalSnapshot(){
        record(date,"09:30",null);var r=request();
        assertThat(attendance.findAll().get(0).getCheckOut()).isNull();
        service.review("boss",r.getId(),true,"확인");
        var a=attendance.findAll().get(0);assertThat(a.getCheckIn()).isEqualTo(date.atTime(9,0));assertThat(a.getCheckOut()).isEqualTo(date.atTime(18,0));assertThat(a.isLateArrival()).isFalse();
        var saved=corrections.findById(r.getId()).orElseThrow();assertThat(saved.getOriginalCheckIn()).isEqualTo(date.atTime(9,30));assertThat(saved.getOriginalCheckOut()).isNull();assertThat(saved.getReviewer().getId()).isEqualTo(boss.getId());
        assertThatThrownBy(()->service.review("boss",r.getId(),true,"")).isInstanceOf(BusinessException.class);
        assertThat(reports.monthly("boss",YearMonth.of(2026,9),"staff").rows().get(0).totals().workMinutes()).isEqualTo(480);
    }
    @Test void missingAttendanceCanBeCreatedOnlyAfterApprovalAndRejectionLeavesItMissing(){
        var r=request();service.review("boss",r.getId(),false,"증빙 확인 필요");assertThat(attendance.count()).isZero();
        service.request("staff",boss.getId(),date,LocalTime.of(9,0),LocalTime.of(18,0),"재신청");
        var pending=corrections.findAll().stream().filter(c->c.getStatus()==AttendanceCorrection.Status.PENDING).findFirst().orElseThrow();
        service.review("boss",pending.getId(),true,"");assertThat(attendance.count()).isEqualTo(1);
    }
    @Test void duplicateFutureSelfAndCrossDepartmentRequestsAreRejected(){
        request();assertThatThrownBy(this::request).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->service.request("staff",outsider.getId(),date.plusDays(1),LocalTime.of(9,0),LocalTime.of(18,0),"사유")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->service.request("boss",boss.getId(),date,LocalTime.of(9,0),LocalTime.of(18,0),"사유")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->service.request("staff",boss.getId(),date.plusDays(1),LocalTime.of(9,0),LocalTime.of(20,0),"미래 시간")).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->service.request("staff",boss.getId(),date.minusDays(1),LocalTime.of(9,0),LocalTime.of(18,0),"입사 전")).isInstanceOf(BusinessException.class);
        var r=corrections.findAll().get(0);assertThatThrownBy(()->service.cancel("boss",r.getId())).isInstanceOf(BusinessException.class);
        assertThatThrownBy(()->service.review("outside",r.getId(),true,"")).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        service.cancel("staff",r.getId());assertThat(corrections.findById(r.getId()).orElseThrow().getStatus()).isEqualTo(AttendanceCorrection.Status.CANCELLED);
    }
    @Test void changedRecordCannotBeOverwritten(){
        var a=record(date,"09:30",null);var r=request();a.setCheckOut(date.atTime(19,0));attendance.saveAndFlush(a);
        assertThatThrownBy(()->service.review("boss",r.getId(),true,"")).isInstanceOf(BusinessException.class).hasMessageContaining("변경");
        assertThat(attendance.findById(a.getId()).orElseThrow().getCheckOut()).isEqualTo(date.atTime(19,0));
        assertThat(corrections.findById(r.getId()).orElseThrow().getStatus()).isEqualTo(AttendanceCorrection.Status.PENDING);
        service.review("boss",r.getId(),false,"원본 변경으로 재신청");
    }
    @Test void deletionCancelsOwnRequestsAndRemovedApproversCanBeReplaced(){
        var r=request();var next=portal.createEmployee("nextboss","Manager123!","대체부장","개발","부장",Employee.Role.EMPLOYEE,"","",date,true);
        portal.deleteEmployee("admin",boss.getId(),"boss");service.review("nextboss",r.getId(),true,"");
        assertThat(corrections.findById(r.getId()).orElseThrow().getReviewer().getId()).isEqualTo(next.getId());
        service.request("staff",next.getId(),date.plusDays(1),LocalTime.of(9,0),LocalTime.of(18,0),"누락");
        portal.deleteEmployee("admin",staff.getId(),"staff");
        assertThat(corrections.findAll()).extracting(AttendanceCorrection::getStatus).containsExactlyInAnyOrder(AttendanceCorrection.Status.APPROVED,AttendanceCorrection.Status.CANCELLED);
        assertThat(attendance.count()).isEqualTo(1);
    }
    @Test void allNewPagesRenderAndRoutesEnforceRoleCsrfAndDepartmentScope()throws Exception{
        mvc.perform(get("/employee/correction-request").with(user("staff").roles("EMPLOYEE"))).andExpect(status().isOk());
        for(String page:List.of("leave-calendar","monthly-report","approval"))mvc.perform(get("/manager/"+page).with(user("boss").roles("EMPLOYEE"))).andExpect(status().isOk());
        mvc.perform(get("/manager/monthly-report.xlsx").with(user("staff").roles("EMPLOYEE"))).andExpect(status().isForbidden());
        mvc.perform(get("/manager/leave-calendar").with(user("admin").roles("ADMIN"))).andExpect(status().isForbidden());
        mvc.perform(get("/manager/monthly-report").param("month","invalid").with(user("boss").roles("EMPLOYEE"))).andExpect(status().isBadRequest());
        mvc.perform(post("/employee/corrections").with(user("staff").roles("EMPLOYEE"))).andExpect(status().isForbidden());
        mvc.perform(post("/employee/corrections").with(user("staff").roles("EMPLOYEE")).with(csrf()).param("approverId",boss.getId().toString()).param("workDate",date.toString()).param("startTime","09:00").param("endTime","18:00").param("reason","누락"))
            .andExpect(redirectedUrl("/employee/request-status"));
        mvc.perform(get("/employee/request-status").with(user("staff").roles("EMPLOYEE"))).andExpect(status().isOk());
        mvc.perform(get("/manager/approval").with(user("boss").roles("EMPLOYEE"))).andExpect(status().isOk());
        var r=corrections.findAll().get(0);
        mvc.perform(post("/manager/corrections/"+r.getId()+"/review").with(user("boss").roles("EMPLOYEE")).with(csrf()).param("approve","true")).andExpect(redirectedUrl("/manager/approval"));
    }
    LeaveRequest leave(Employee e,LocalDate start,LocalDate end,LeaveRequest.Kind kind,LeaveRequest.Status status){var r=new LeaveRequest();r.setEmployee(e);r.setApprover(boss);r.setStartDate(start);r.setEndDate(end);r.setKind(kind);r.setStatus(status);r.setReason("private reason");r.setRequestedAt(date.atStartOfDay());r.setChargedUnits(2);return leaves.save(r);}
    @Test void calendarShowsOnlyApprovedDepartmentLeaveAcrossMonthBoundaryAndNoReasons()throws Exception{
        leave(staff,LocalDate.of(2026,8,31),date,LeaveRequest.Kind.ANNUAL,LeaveRequest.Status.APPROVED);
        leave(staff,date.plusDays(1),date.plusDays(1),LeaveRequest.Kind.HALF_AM,LeaveRequest.Status.PENDING);
        leave(outsider,date,date,LeaveRequest.Kind.SICK,LeaveRequest.Status.APPROVED);
        var calendar=reports.calendar("boss",YearMonth.of(2026,9));assertThat(calendar.requestCount()).isEqualTo(1);
        assertThat(calendar.days().stream().filter(d->d.date().equals(date)).findFirst().orElseThrow().items()).hasSize(1);
        assertThat(calendar.days().stream().filter(d->d.date().getDayOfWeek()==DayOfWeek.SUNDAY).flatMap(d->d.items().stream())).isEmpty();
        mvc.perform(get("/manager/leave-calendar").with(user("boss").roles("EMPLOYEE"))).andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("private reason"))));
    }
    @Test void reportUsesSelectedMonthHalfLeaveAndExportsSafeNumericWorkbook()throws Exception{
        record(date,"09:00","20:00");record(date.plusDays(1),"14:00","18:00");
        leave(staff,date.plusDays(1),date.plusDays(1),LeaveRequest.Kind.HALF_AM,LeaveRequest.Status.APPROVED);
        var report=reports.monthly("boss",YearMonth.of(2026,9),"staff");var t=report.rows().get(0).totals();
        assertThat(t.workMinutes()).isEqualTo(840);assertThat(t.overtimeMinutes()).isEqualTo(120);assertThat(t.lateDays()).isZero();assertThat(t.leaveDays()).isEqualTo(0.5);
        assertThat(reports.monthly("boss",YearMonth.of(2026,8),"").rows()).isEmpty();
        assertThat(reports.monthly("outside",YearMonth.of(2026,9),"staff").rows()).isEmpty();
        portal.updateEmployee("admin",staff.getId(),"=1+1<&",staff.getDepartment(),"사원",true,15);
        var result=mvc.perform(get("/manager/monthly-report.xlsx").param("month","2026-09").param("query","staff").with(user("boss").roles("EMPLOYEE")))
            .andExpect(status().isOk()).andExpect(header().string("Content-Disposition","attachment; filename=\"attendance-2026-09.xlsx\"" )).andReturn();
        var files=new HashMap<String,String>();
        try(var zip=new ZipInputStream(new ByteArrayInputStream(result.getResponse().getContentAsByteArray()))){for(var entry=zip.getNextEntry();entry!=null;entry=zip.getNextEntry()){var bytes=zip.readAllBytes();javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(bytes));files.put(entry.getName(),new String(bytes,java.nio.charset.StandardCharsets.UTF_8));}}
        assertThat(files).hasSize(5);assertThat(files.get("xl/worksheets/sheet1.xml")).contains("=1+1&lt;&amp;","<v>840</v>","<v>120</v>").doesNotContain("<f>","외부부장");
    }
}
