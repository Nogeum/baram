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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import java.time.*;
import java.math.BigDecimal;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test") @Transactional
class GroupwareTests {
    @Autowired PortalService portal;
    @Autowired GroupwareService service;
    @Autowired DepartmentReportService departmentReports;
    @Autowired AmendmentService amendments;
    @Autowired WorkCalendarService calendar;
    @Autowired AttendanceReportService reports;
    @Autowired OvertimeService overtime;
    @Autowired AttendanceCorrectionService correction;
    @Autowired WorkTaskRepository tasks;
    @Autowired ApprovalDocumentRepository documents;
    @Autowired LeaveRequestRepository leaves;
    @Autowired OvertimeRequestRepository overtimes;
    @Autowired AttendanceCorrectionRepository corrections;
    @Autowired AttendanceRepository attendance;
    @Autowired RequestAmendmentRepository changes;
    @Autowired BookableResourceRepository resources;
    @Autowired ResourceReservationRepository reservations;
    @Autowired StoredFileRepository files;
    @Autowired NotificationRepository notifications;
    @Autowired CompanyHolidayRepository holidays;
    @Autowired MockMvc mvc;
    @Autowired EntityManager em;
    @Autowired EmployeeRepository employeeRepository;
    @Autowired org.springframework.transaction.PlatformTransactionManager transactionManager;
    @MockitoBean Clock clock;
    Employee admin,manager,second,worker,outside;
    @BeforeEach void setup(){
        when(clock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));when(clock.instant()).thenReturn(Instant.parse("2026-09-24T13:00:00Z"));
        admin=create("gwadmin","인사",Employee.Role.ADMIN,false);
        manager=create("gwmanager","개발",Employee.Role.EMPLOYEE,true);
        second=create("gwsecond","경영",Employee.Role.EMPLOYEE,true);
        worker=create("gwworker","개발",Employee.Role.EMPLOYEE,false);
        outside=create("gwoutside","영업",Employee.Role.EMPLOYEE,false);
    }
    Employee create(String login,String department,Employee.Role role,boolean manager){login=login+"_"+UUID.randomUUID().toString().substring(0,8);return portal.createEmployee(login,"Testing123!",login,department,"직원",role,"","",LocalDate.of(2026,1,1),manager);}
    @Test void departmentCalendarReflectsTaskDatesAndStatusWithoutExposingOtherDepartments() throws Exception {
        var due=LocalDate.of(2026,9,26); // Weekend tasks must still appear.
        var id=service.saveTask(worker.getLoginId(),null,worker.getId(),"부서 캘린더 업무","비공개 상세",due);
        var foreign=service.saveTask(outside.getLoginId(),null,outside.getId(),"외부 부서 업무","상세",due);
        service.taskStatus(worker.getLoginId(),id,"IN_PROGRESS");
        var result=departmentReports.calendar(manager.getLoginId(),YearMonth.of(2026,9));
        var day=result.days().stream().filter(d->d.date().equals(due)).findFirst().orElseThrow();
        assertThat(day.tasks()).extracting(DepartmentReportService.TaskItem::id).contains(id).doesNotContain(foreign);
        var task=day.tasks().stream().filter(t->t.id().equals(id)).findFirst().orElseThrow();
        assertThat(task.status()).isEqualTo("IN_PROGRESS");assertThat(task.own()).isFalse();
        mvc.perform(get("/manager/leave-calendar").param("month","2026-09").with(user(manager.getLoginId()).roles("EMPLOYEE")))
            .andExpect(status().isOk()).andExpect(content().string(org.hamcrest.Matchers.containsString("부서 캘린더 업무")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("외부 부서 업무"))))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("비공개 상세"))));
        assertThatThrownBy(()->departmentReports.calendar(worker.getLoginId(),YearMonth.of(2026,9))).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        assertThatThrownBy(()->service.task(manager,id)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        service.saveTask(worker.getLoginId(),id,worker.getId(),"마감 변경 업무","상세",LocalDate.of(2026,10,1));
        assertThat(departmentReports.calendar(manager.getLoginId(),YearMonth.of(2026,9)).days().stream().flatMap(d->d.tasks().stream()).map(DepartmentReportService.TaskItem::id)).doesNotContain(id);
        service.taskStatus(worker.getLoginId(),id,"DONE");
        assertThat(departmentReports.calendar(manager.getLoginId(),YearMonth.of(2026,10)).days().stream().flatMap(d->d.tasks().stream()).filter(t->t.id().equals(id)).findFirst().orElseThrow().status()).isEqualTo("DONE");
    }
    @Test void employeeFiltersKeepDepartmentRoleAndStatusIndependent() throws Exception {
        mvc.perform(get("/admin/employee-manage").with(user(admin.getLoginId()).roles("ADMIN"))
                .param("departmentFilter","개발").param("roleFilter","EMPLOYEE").param("activeFilter","true").param("query",worker.getLoginId()))
            .andExpect(status().isOk()).andExpect(model().attribute("employees",org.hamcrest.Matchers.hasSize(1)))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("employeeCreateDrawer")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("data-login=\""+worker.getLoginId()+"\"")))
            .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("data-login=\""+outside.getLoginId()+"\""))));
    }
    @Test @Transactional(propagation=org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    void concurrentReservationsAllowOnlyOneWinner() throws Exception {
        service.saveResource(admin.getLoginId(),null,"동시 예약 검증","ROOM","테스트",2,true);
        var resource=resources.findAll().stream().filter(r->r.getName().equals("동시 예약 검증")).findFirst().orElseThrow();
        var start=portal.today().plusDays(1).atTime(10,0);
        var executor=java.util.concurrent.Executors.newFixedThreadPool(2);var gate=new java.util.concurrent.CountDownLatch(1);
        try {
            java.util.concurrent.Callable<Boolean> first=()->{gate.await();try{service.reserve(worker.getLoginId(),resource.getId(),"동시1",start,start.plusHours(1));return true;}catch(BusinessException e){return false;}};
            java.util.concurrent.Callable<Boolean> secondCall=()->{gate.await();try{service.reserve(outside.getLoginId(),resource.getId(),"동시2",start,start.plusHours(1));return true;}catch(BusinessException e){return false;}};
            var a=executor.submit(first);var b=executor.submit(secondCall);gate.countDown();
            assertThat(List.of(a.get(20,java.util.concurrent.TimeUnit.SECONDS),b.get(20,java.util.concurrent.TimeUnit.SECONDS))).containsExactlyInAnyOrder(true,false);
        } finally {
            executor.shutdownNow();
            new org.springframework.transaction.support.TransactionTemplate(transactionManager).executeWithoutResult(status->{
                reservations.deleteAll(reservations.findAll().stream().filter(r->r.getResource().getId().equals(resource.getId())).toList());
                resources.deleteById(resource.getId());
                var ids=List.of(admin.getId(),manager.getId(),second.getId(),worker.getId(),outside.getId());
                notifications.deleteAll(notifications.findAll().stream().filter(n->ids.contains(n.getRecipient().getId())).toList());
                employeeRepository.deleteAllById(ids);
            });
        }
    }
    @Test void allNewScreensRenderForAllRoles() throws Exception{
        for(var e:List.of(admin,manager,worker))for(String page:List.of("tasks","documents","directory","holidays","reservations","library")){
            mvc.perform(get("/groupware/"+page).with(user(e.getLoginId()).roles(e.getRole().name()))).andExpect(e==admin&&List.of("tasks","documents").contains(page)?status().isForbidden():status().isOk());
        }
        mvc.perform(get("/groupware/tasks")).andExpect(status().is3xxRedirection());
        mvc.perform(post("/groupware/tasks").with(user(worker.getLoginId()).roles("EMPLOYEE"))).andExpect(status().isForbidden());
    }
    @Test void tasksCommentsAttachmentsAndRemindersRespectDepartment() throws Exception{
        Long id=service.saveTask(worker.getLoginId(),null,worker.getId(),"릴리스 점검","상세 점검",portal.today());
        service.comment(worker.getLoginId(),id,"진행하겠습니다.");service.taskStatus(worker.getLoginId(),id,"IN_PROGRESS");
        service.upload(worker.getLoginId(),"TASK",id,new MockMultipartFile("file","보고서.txt","text/plain","테스트".getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        var file=service.files(worker,"TASK",id).get(0);
        mvc.perform(get("/groupware/tasks/"+id).with(user(worker.getLoginId()).roles("EMPLOYEE"))).andExpect(status().isOk());
        mvc.perform(get("/groupware/files/"+file.getId()).with(user(outside.getLoginId()).roles("EMPLOYEE"))).andExpect(status().isForbidden());
        assertThatThrownBy(()->service.comment(outside.getLoginId(),id,"접근")).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        long before=notifications.count();service.remindTasks();service.remindTasks();assertThat(notifications.count()).isEqualTo(before+1);
        service.taskStatus(worker.getLoginId(),id,"DONE");assertThat(tasks.findById(id).orElseThrow().getStatus()).isEqualTo("DONE");
        assertThat(service.tasks(worker,"","DONE")).extracting(WorkTask::getId).contains(id);
        assertThat(service.tasks(outside,"","")).extracting(WorkTask::getId).doesNotContain(id);
    }
    @Test void sequentialApprovalsAndDocumentPrivacy() throws Exception{
        Long id=service.createDocument(worker.getLoginId(),"PURCHASE","노트북 구매","업무용",new BigDecimal("1200000"),null,null,List.of(manager.getId(),second.getId()));
        assertThat(service.canReview(second,documents.findById(id).orElseThrow())).isFalse();
        mvc.perform(post("/groupware/documents/"+id+"/review").with(user(second.getLoginId()).roles("EMPLOYEE")).with(csrf()).param("approve","true")).andExpect(status().isForbidden());
        mvc.perform(get("/groupware/documents/"+id).with(user(worker.getLoginId()).roles("EMPLOYEE"))).andExpect(status().isOk());
        mvc.perform(get("/groupware/documents/"+id).with(user(outside.getLoginId()).roles("EMPLOYEE"))).andExpect(status().isForbidden());
        service.reviewDocument(manager.getLoginId(),id,true,"확인");assertThat(documents.findById(id).orElseThrow().getCurrentStep()).isEqualTo(2);
        service.reviewDocument(second.getLoginId(),id,true,"최종 승인");assertThat(documents.findById(id).orElseThrow().getStatus()).isEqualTo("APPROVED");
        assertThat(service.steps(id)).allMatch(s->s.getStatus().equals("APPROVED"));
    }
    @Test void approvalFormAcceptsOptionalEmptyReviewers() throws Exception{
        mvc.perform(post("/groupware/documents").with(user(worker.getLoginId()).roles("EMPLOYEE")).with(csrf())
            .param("kind","EXPENSE").param("title","지출 결의").param("content","영수증").param("amount","12000")
            .param("reviewerIds",manager.getId().toString(),"","","","")).andExpect(status().is3xxRedirection());
    }
    @Test void libraryScopeAndDirectoryUpdates() throws Exception{
        Long id=service.saveLibrary(worker.getLoginId(),null,"부서 매뉴얼","업무 절차","DEPARTMENT");
        service.upload(worker.getLoginId(),"LIBRARY",id,new MockMultipartFile("file","manual.txt","text/plain",new byte[]{1,2,3}));
        assertThat(service.library(outside,"")).extracting(SharedDocument::getId).doesNotContain(id);
        mvc.perform(get("/groupware/library/"+id).with(user(worker.getLoginId()).roles("EMPLOYEE"))).andExpect(status().isOk());
        mvc.perform(get("/groupware/files/"+service.files(worker,"LIBRARY",id).get(0).getId()).with(user(outside.getLoginId()).roles("EMPLOYEE"))).andExpect(status().isForbidden());
        service.saveLibrary(worker.getLoginId(),id,"부서 매뉴얼","업무 절차","COMPANY");assertThat(service.library(outside,"")).extracting(SharedDocument::getId).contains(id);
        service.profile(worker.getLoginId(),worker.getId(),"1234","서버 운영");assertThat(service.directory("서버 운영")).extracting(Employee::getId).contains(worker.getId());
        assertThatThrownBy(()->service.profile(outside.getLoginId(),worker.getId(),"9999","변경")).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
    @Test void resourcesRejectOverlapAndUnauthorizedCancellation() throws Exception{
        service.saveResource(admin.getLoginId(),null,"회의실A","ROOM","2층",8,true);
        var resource=resources.findAll().stream().filter(r->r.getName().equals("회의실A")).findFirst().orElseThrow();
        var start=portal.today().plusDays(1).atTime(10,0);
        service.reserve(worker.getLoginId(),resource.getId(),"회의",start,start.plusHours(1));
        assertThatThrownBy(()->service.reserve(outside.getLoginId(),resource.getId(),"중복",start.plusMinutes(30),start.plusHours(2))).isInstanceOf(BusinessException.class).hasMessageContaining("겹칩니다");
        service.reserve(outside.getLoginId(),resource.getId(),"다음 회의",start.plusHours(1),start.plusHours(2));
        var booking=reservations.findAll().stream().filter(r->r.getEmployee().getId().equals(worker.getId())).findFirst().orElseThrow();
        assertThatThrownBy(()->service.cancelReservation(outside.getLoginId(),booking.getId())).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        service.cancelReservation(worker.getLoginId(),booking.getId());assertThat(booking.getStatus()).isEqualTo("CANCELLED");
        mvc.perform(get("/groupware/reservations").param("date",start.toLocalDate().toString()).with(user(worker.getLoginId()).roles("EMPLOYEE"))).andExpect(status().isOk());
    }
    @Test void personalTasksRejectOtherAssigneesEvenForManagers(){
        for(var actor:List.of(manager,admin,worker))assertThatThrownBy(()->service.saveTask(actor.getLoginId(),null,outside.getId(),"권한 확인","다른 사람 지정",portal.today())).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        var id=service.saveTask(worker.getLoginId(),null,worker.getId(),"내 업무","개인 작업",portal.today());
        assertThatThrownBy(()->service.task(manager,id)).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
    }
    @Test void reservationRequiresAdministratorReviewAndKeepsAudit(){
        service.saveResource(admin.getLoginId(),null,"허가 테스트","ROOM","2층",4,true);
        var resource=resources.findAll().stream().filter(r->r.getName().equals("허가 테스트")).findFirst().orElseThrow();
        var start=portal.today().plusDays(2).atTime(10,0);
        service.reserve(worker.getLoginId(),resource.getId(),"허가 신청",start,start.plusHours(1));
        var row=reservations.findAll().stream().filter(r->r.getResource().getId().equals(resource.getId())).findFirst().orElseThrow();
        assertThat(row.getStatus()).isEqualTo("PENDING");assertThat(service.reservations(portal.today(),true)).contains(row);
        assertThatThrownBy(()->service.reviewReservation(manager.getLoginId(),row.getId(),true,"")).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);
        service.reviewReservation(admin.getLoginId(),row.getId(),true,"허가");assertThat(row.getStatus()).isEqualTo("ACTIVE");assertThat(row.getReviewer()).isEqualTo(admin);assertThat(row.getReviewedAt()).isNotNull();
        assertThatThrownBy(()->service.reviewReservation(admin.getLoginId(),row.getId(),false,"중복 처리")).isInstanceOf(BusinessException.class);
        service.reserve(outside.getLoginId(),resource.getId(),"반려 신청",start.plusHours(2),start.plusHours(3));
        var rejected=reservations.findAll().stream().filter(r->r.getResource().getId().equals(resource.getId())&&r.getStatus().equals("PENDING")).findFirst().orElseThrow();
        assertThatThrownBy(()->service.reviewReservation(admin.getLoginId(),rejected.getId(),false,"")).isInstanceOf(BusinessException.class);
        service.reviewReservation(admin.getLoginId(),rejected.getId(),false,"점검 시간");assertThat(rejected.getStatus()).isEqualTo("REJECTED");
        service.reserve(worker.getLoginId(),resource.getId(),"재신청",start.plusHours(2),start.plusHours(3));
    }
    @Autowired DepartmentAccess departmentAccess;
    @Test void divisionHeadHasHigherApprovalAuthority(){
        var head=create("gwhead","개발",Employee.Role.EMPLOYEE,true);head.setPositionName("본부장");
        assertThat(head.isDivisionHead()).isTrue();
        assertThat(departmentAccess.canReview(head,worker,manager)).isTrue();
        assertThat(departmentAccess.canReview(manager,worker,head)).isFalse();
        assertThat(departmentAccess.canReview(head,head,manager)).isFalse();
        assertThat(departmentAccess.canReview(head,outside,second)).isFalse();
        assertThatThrownBy(()->service.createDocument(worker.getLoginId(),"EXPENSE","상위 결재","내역",BigDecimal.TEN,null,null,List.of(manager.getId()))).isInstanceOf(BusinessException.class);
        var id=service.createDocument(worker.getLoginId(),"EXPENSE","상위 결재","내역",BigDecimal.TEN,null,null,List.of(manager.getId(),head.getId()));
        assertThat(service.canReview(head,documents.findById(id).orElseThrow())).isFalse();
        service.reviewDocument(manager.getLoginId(),id,true,"");service.reviewDocument(head.getLoginId(),id,true,"");assertThat(documents.findById(id).orElseThrow().getStatus()).isEqualTo("APPROVED");
    }
    @Test void holidaysRecalculateLeaveAndExcludeAbsence(){
        var day=portal.today().plusDays(1);
        portal.requestLeave(worker.getLoginId(),manager.getId(),LeaveRequest.Kind.ANNUAL,day,day,"휴가");
        var leave=leaves.findByEmployeeIdOrderByRequestedAtDesc(worker.getId()).get(0);assertThat(leave.getChargedUnits()).isEqualTo(2);
        calendar.save(admin,day,"회사 휴무","COMPANY");assertThat(leave.getChargedUnits()).isZero();assertThat(portal.remaining(worker,day.getYear())).isEqualTo(15);
        var holiday=holidays.findAll().stream().filter(h->h.getHolidayDate().equals(day)).findFirst().orElseThrow();calendar.delete(admin,holiday.getId());assertThat(leave.getChargedUnits()).isEqualTo(2);
        var before=reports.monthly(worker,YearMonth.from(portal.today()),List.of(),List.of()).absentDays();
        calendar.save(admin,portal.today().minusDays(1),"휴무","COMPANY");assertThat(reports.monthly(worker,YearMonth.from(portal.today()),List.of(),List.of()).absentDays()).isEqualTo(before-1);
    }
    Long approvedLeave(){
        var day=portal.today().plusDays(1);portal.requestLeave(worker.getLoginId(),manager.getId(),LeaveRequest.Kind.ANNUAL,day,day,"휴가");
        var r=leaves.findByEmployeeIdOrderByRequestedAtDesc(worker.getId()).get(0);portal.review(manager.getLoginId(),r.getId(),true,"");em.flush();return r.getId();
    }
    @Test void approvedLeaveCancellationRefundsOnlyAfterReview() throws Exception{
        Long original=approvedLeave();Long id=amendments.request(worker.getLoginId(),"LEAVE",original,"CANCEL",manager.getId(),"일정 변경",null,null,null,null,null);
        assertThat(portal.remaining(worker,2026)).isEqualTo(14);
        mvc.perform(get("/employee/request-status").param("type","LEAVE").param("requestId",original.toString()).with(user(worker.getLoginId()).roles("EMPLOYEE"))).andExpect(status().isOk());
        mvc.perform(get("/manager/approval").param("amendmentId",id.toString()).with(user(manager.getLoginId()).roles("EMPLOYEE"))).andExpect(status().isOk());
        amendments.review(manager.getLoginId(),id,true,"취소 승인");assertThat(portal.remaining(worker,2026)).isEqualTo(15);assertThat(leaves.findById(original).orElseThrow().getStatus()).isEqualTo(LeaveRequest.Status.CANCELLED);
    }
    @Test void approvedLeaveChangeKeepsOriginalAndReplacement(){
        Long original=approvedLeave();var day=portal.today().plusDays(4);
        Long id=amendments.request(worker.getLoginId(),"LEAVE",original,"CHANGE",manager.getId(),"휴가 변경",day,day,"HALF_AM",null,null);
        amendments.review(manager.getLoginId(),id,true,"변경 승인");
        var changed=changes.findById(id).orElseThrow();assertThat(changed.getReplacementId()).isNotNull();
        assertThat(leaves.findById(changed.getReplacementId()).orElseThrow().getStatus()).isEqualTo(LeaveRequest.Status.APPROVED);
        assertThat(portal.remaining(worker,2026)).isEqualTo(14.5);
        assertThat(leaves.findById(original).orElseThrow().getStartDate()).isEqualTo(portal.today().plusDays(1));
    }
    @Test void overtimeAndCorrectionCanBeReversed(){
        var day=portal.today().minusDays(1);overtime.request(worker.getLoginId(),manager.getId(),day,LocalTime.of(18,0),LocalTime.of(20,0),"작업");
        var ot=overtimes.findByEmployeeIdOrderByRequestedAtDesc(worker.getId()).get(0);overtime.review(manager.getLoginId(),ot.getId(),true,"");em.flush();
        Long oid=amendments.request(worker.getLoginId(),"OVERTIME",ot.getId(),"CANCEL",manager.getId(),"취소",null,null,null,null,null);amendments.review(manager.getLoginId(),oid,true,"");assertThat(ot.getStatus()).isEqualTo(OvertimeRequest.Status.CANCELLED);
        correction.request(worker.getLoginId(),manager.getId(),day,LocalTime.of(9,0),LocalTime.of(18,0),"누락");
        var c=corrections.findByEmployeeIdOrderByRequestedAtDesc(worker.getId()).get(0);correction.review(manager.getLoginId(),c.getId(),true,"");em.flush();
        Long cid=amendments.request(worker.getLoginId(),"CORRECTION",c.getId(),"CANCEL",manager.getId(),"취소",null,null,null,null,null);amendments.review(manager.getLoginId(),cid,true,"");
        assertThat(attendance.findByEmployeeIdAndWorkDate(worker.getId(),day)).isEmpty();assertThat(c.getStatus()).isEqualTo(AttendanceCorrection.Status.CANCELLED);
    }
    @Test void changedAttendanceBlocksOldCorrectionReversal(){
        var day=portal.today().minusDays(1);correction.request(worker.getLoginId(),manager.getId(),day,LocalTime.of(9,0),LocalTime.of(18,0),"누락");
        var c=corrections.findByEmployeeIdOrderByRequestedAtDesc(worker.getId()).get(0);correction.review(manager.getLoginId(),c.getId(),true,"");em.flush();
        Long id=amendments.request(worker.getLoginId(),"CORRECTION",c.getId(),"CANCEL",manager.getId(),"취소",null,null,null,null,null);
        var record=attendance.findByEmployeeIdAndWorkDate(worker.getId(),day).orElseThrow();record.setCheckOut(day.atTime(19,0));em.flush();
        assertThatThrownBy(()->amendments.review(manager.getLoginId(),id,true,"")).isInstanceOf(BusinessException.class).hasMessageContaining("변경되었습니다");
        assertThat(record.getCheckOut()).isEqualTo(day.atTime(19,0));
    }
}
