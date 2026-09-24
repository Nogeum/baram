package org.example.portal.web;

import lombok.RequiredArgsConstructor;
import org.example.portal.domain.*;
import org.example.portal.repository.*;
import org.example.portal.service.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.format.annotation.DateTimeFormat;
import java.security.Principal;
import java.time.*;
import java.util.*;

@Controller @RequiredArgsConstructor
public class DepartmentController {
    private final DepartmentAccess departments;
    private final PortalService service;
    private final AmendmentView amendmentView;
    private final OvertimeService overtimeService;
    private final AttendanceCorrectionService correctionService;
    private final AttendanceCorrectionRepository corrections;
    private final DepartmentReportService reports;
    private final EmployeeRepository employees;
    private final AttendanceRepository attendance;
    private final ScheduleRepository schedules;
    private final LeaveRequestRepository leaves;
    private final OvertimeRequestRepository overtime;
    private final NoticeRepository notices;
    private final NotificationRepository notifications;
    private static final Map<String,String> PAGES=Map.of("main","부서 대시보드","attendance-manage","부서 근태관리","schedule-assign","스케줄 배정","approval","승인 대기함","leave-calendar","부서 캘린더","monthly-report","월별 근태 보고서");

    @GetMapping("/manager/{page}") String page(@PathVariable String page,Principal principal,Model model,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date,@RequestParam(required=false) String month,@RequestParam(defaultValue="") String query,@RequestParam(required=false) Long amendmentId) {
        var me=departments.manager(principal.getName());
        if(!PAGES.containsKey(page)) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
        var today=service.today(); date=month==null ? (date==null ? today : date) : parseMonth(month).atDay(1); var selectedMonth=YearMonth.from(date);
        var members=employees.findByDepartmentAndRoleOrderByNameAsc(me.getDepartment(),Employee.Role.EMPLOYEE);
        var requests=leaves.findByEmployeeDepartmentAndEmployeeRoleAndStatusOrderByRequestedAtAsc(me.getDepartment(),Employee.Role.EMPLOYEE,LeaveRequest.Status.PENDING).stream()
            .filter(r->departments.canReview(me,r.getEmployee(),r.getApprover())).toList();
        var overtimeRequests=overtime.findByEmployeeDepartmentAndEmployeeRoleAndStatusOrderByRequestedAtAsc(me.getDepartment(),Employee.Role.EMPLOYEE,OvertimeRequest.Status.PENDING).stream()
            .filter(r->departments.canReview(me,r.getEmployee(),r.getApprover())).toList();
        model.addAttribute("me",me); model.addAttribute("admin",true); model.addAttribute("managerView",true); model.addAttribute("base","/manager");
        model.addAttribute("page",page); model.addAttribute("title",PAGES.get(page)); model.addAttribute("today",today); model.addAttribute("date",date); model.addAttribute("year",today.getYear()); model.addAttribute("query",query);
        var correctionRequests=corrections.findByEmployeeDepartmentAndStatusOrderByRequestedAtAsc(me.getDepartment(),AttendanceCorrection.Status.PENDING).stream()
            .filter(r->departments.canReview(me,r.getEmployee(),r.getApprover())).toList();
        model.addAttribute("correctionRequests",correctionRequests);
        model.addAttribute("requests",requests); model.addAttribute("overtimeRequests",overtimeRequests); model.addAttribute("pending",requests.size()+overtimeRequests.size()+correctionRequests.size()+amendmentView.pending(me,true));
        model.addAttribute("employeeCount",members.stream().filter(Employee::isActive).count());
        model.addAttribute("attendedCount",attendance.findByEmployeeDepartmentAndEmployeeRoleAndWorkDateOrderByEmployeeNameAsc(me.getDepartment(),Employee.Role.EMPLOYEE,today).size());
        model.addAttribute("attendanceRecords",attendance.findByEmployeeDepartmentAndEmployeeRoleAndWorkDateOrderByEmployeeNameAsc(me.getDepartment(),Employee.Role.EMPLOYEE,date).stream()
            .filter(a->query.isBlank() || a.getEmployee().getName().contains(query) || a.getEmployee().getLoginId().contains(query)).toList());
        model.addAttribute("activeEmployees",members.stream().filter(Employee::isActive).toList());
        model.addAttribute("schedules",schedules.findByEmployeeDepartmentAndEmployeeRoleAndAssignedTrueAndEventDateBetweenOrderByEventDateAscStartTimeAsc(me.getDepartment(),Employee.Role.EMPLOYEE,selectedMonth.atDay(1),selectedMonth.atEndOfMonth()));
        if(page.equals("monthly-report"))model.addAttribute("monthlyReport",reports.monthly(principal.getName(),selectedMonth,query));
        if(page.equals("leave-calendar"))model.addAttribute("leaveCalendar",reports.calendar(principal.getName(),selectedMonth));
        model.addAttribute("notices",notices.findTop20ByOrderByCreatedAtDesc());
        model.addAttribute("notifications",notifications.findTop100ByRecipientIdOrderByCreatedAtDesc(me.getId()));
        model.addAttribute("unreadNotifications",notifications.countByRecipientIdAndReadFlagFalse(me.getId()));
        if(page.equals("approval")) amendmentView.populate(model,me,true,null,null,amendmentId);
        return "portal";
    }
    @PostMapping("/manager/leave/{id}/review") String reviewLeave(Principal p,@PathVariable Long id,@RequestParam boolean approve,@RequestParam(defaultValue="") String comment,RedirectAttributes f) {
        service.review(p.getName(),id,approve,comment); return done(f,"/manager/approval");
    }
    @PostMapping("/manager/corrections/{id}/review") String reviewCorrection(Principal p,@PathVariable Long id,@RequestParam boolean approve,@RequestParam(defaultValue="") String comment,RedirectAttributes f){correctionService.review(p.getName(),id,approve,comment);return done(f,"/manager/approval");}
    @GetMapping("/manager/monthly-report.xlsx") org.springframework.http.ResponseEntity<byte[]> download(Principal p,@RequestParam(required=false) String month,@RequestParam(defaultValue="") String query) {
        var selected=month==null ? YearMonth.from(service.today()) : parseMonth(month);
        var report=reports.monthly(p.getName(),selected,query);
        return org.springframework.http.ResponseEntity.ok()
            .header("Content-Disposition","attachment; filename=\"attendance-"+selected+".xlsx\"")
            .header("Cache-Control","no-store")
            .contentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(AttendanceWorkbook.write(report));
    }
    private YearMonth parseMonth(String value){try{var month=YearMonth.parse(value);if(month.getYear()<1900 || month.getYear()>9998)throw new IllegalArgumentException();return month;}catch(RuntimeException e){throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST,"올바른 조회 월을 선택하세요.");}}
    @PostMapping("/manager/overtime/{id}/review") String reviewOvertime(Principal p,@PathVariable Long id,@RequestParam boolean approve,@RequestParam(defaultValue="") String comment,RedirectAttributes f) {
        overtimeService.review(p.getName(),id,approve,comment); return done(f,"/manager/approval");
    }
    @PostMapping("/manager/schedule") String assign(Principal p,@RequestParam Long employeeId,@RequestParam String title,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate eventDate,
        @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.TIME) LocalTime startTime,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.TIME) LocalTime endTime,@RequestParam(defaultValue="") String memo,RedirectAttributes f) {
        departments.manager(p.getName()); service.addSchedule(p.getName(),employeeId,title,eventDate,startTime,endTime,memo,Schedule.Color.BLUE);
        return done(f,"/manager/schedule-assign?date="+eventDate);
    }
    @PostMapping("/manager/schedule/{id}/delete") String delete(Principal p,@PathVariable Long id,RedirectAttributes f) {
        var date=service.deleteAssignedSchedule(p.getName(),id); return done(f,"/manager/schedule-assign?date="+date);
    }
    private String done(RedirectAttributes flash,String url) { flash.addFlashAttribute("success","처리되었습니다."); return "redirect:"+url; }
}
