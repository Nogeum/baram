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
public class PortalController {
    private final PortalService service;
    private final AmendmentView amendmentView;
    private final DepartmentAccess departments;
    private final org.springframework.security.core.session.SessionRegistry sessionRegistry;
    private final AttendanceReportService attendanceReport;
    private final OvertimeService overtimeService;
    private final AttendanceCorrectionService correctionService;
    private final AttendanceCorrectionRepository corrections;
    private final OvertimeRequestRepository overtimeRequests;
    private final EmployeeRepository employees;
    private final AttendanceRepository attendance;
    private final LeaveRequestRepository leaves;
    private final ScheduleRepository schedules;
    private final NoticeRepository notices;
    private final NotificationRepository notifications;
    private static final Map<String,String> EMPLOYEE_PAGES=Map.of("main","대시보드","schedule","일정관리","attendance-status","근태현황","leave-request","휴가신청","overtime-request","초과근무신청","correction-request","출퇴근 정정 신청","request-status","신청내역","mypage","마이페이지");
    private static final Map<String,String> ADMIN_PAGES=Map.of("main","시스템 관리","employee-manage","직원·권한 관리","notification","공지·알림");
    @GetMapping("/login") String login() { return "login"; }
    @PostMapping("/notifications/{id}/read") @ResponseBody
    org.springframework.http.ResponseEntity<Void> readPopupNotification(Principal principal,@PathVariable Long id) {
        service.readNotification(principal.getName(),id);
        return org.springframework.http.ResponseEntity.noContent().build();
    }
    @GetMapping("/employee/attendance") String attendanceDashboard() { return "redirect:/employee/main"; }
    @GetMapping("/") String home(Principal p) { return "redirect:"+base(service.current(p.getName()))+"/main"; }

    @GetMapping({"/employee/{page}","/admin/{page}"})
    String page(@PathVariable String page, Principal p, Model model,
                @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date,
                @RequestParam(defaultValue="") String query,@RequestParam(required=false) String type,@RequestParam(required=false) Long requestId,@RequestParam(required=false) Long amendmentId,
                @RequestParam(defaultValue="") String departmentFilter,@RequestParam(defaultValue="") String roleFilter,@RequestParam(defaultValue="") String activeFilter) {
        var me=service.current(p.getName()); boolean admin=me.getRole()==Employee.Role.ADMIN;
        var pages=admin ? ADMIN_PAGES : EMPLOYEE_PAGES;
        if(!pages.containsKey(page)) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND);
        var today=service.today(); date=date==null ? today : date;
        model.addAttribute("managerView",false); model.addAttribute("me",me); model.addAttribute("admin",admin); model.addAttribute("base",base(me));
        model.addAttribute("page",page); model.addAttribute("title",pages.get(page)); model.addAttribute("today",today); model.addAttribute("date",date);
        model.addAttribute("query",query); model.addAttribute("year",today.getYear());
        model.addAttribute("remaining",service.remaining(me,today.getYear()));
        model.addAttribute("todayAttendance",attendance.findByEmployeeIdAndWorkDate(me.getId(),today).orElse(null));
        model.addAttribute("notices",notices.findTop20ByOrderByCreatedAtDesc());
        model.addAttribute("notifications",notifications.findTop100ByRecipientIdOrderByCreatedAtDesc(me.getId()));
        model.addAttribute("unreadNotifications",notifications.countByRecipientIdAndReadFlagFalse(me.getId()));
        var requests=admin ? List.<LeaveRequest>of() : leaves.findByEmployeeIdOrderByRequestedAtDesc(me.getId());
        var overtime=admin ? List.<OvertimeRequest>of() : overtimeRequests.findByEmployeeIdOrderByRequestedAtDesc(me.getId());
        model.addAttribute("overtimeRequests",overtime);
        var correctionList=admin ? List.<AttendanceCorrection>of() : corrections.findByEmployeeIdOrderByRequestedAtDesc(me.getId());
        model.addAttribute("correctionRequests",correctionList);
        model.addAttribute("requests",requests); model.addAttribute("pending",requests.stream().filter(r->r.getStatus()==LeaveRequest.Status.PENDING).count()+overtime.stream().filter(r->r.getStatus()==OvertimeRequest.Status.PENDING).count()+correctionList.stream().filter(r->r.getStatus()==AttendanceCorrection.Status.PENDING).count()+(admin?0:amendmentView.pending(me,false)));
        model.addAttribute("kinds",LeaveRequest.Kind.values());
        model.addAttribute("attendanceRecords",admin ? List.<Attendance>of() : attendance.findTop7ByEmployeeIdOrderByWorkDateDesc(me.getId()));
        var month=YearMonth.from(date);
        var monthSchedules=admin ? List.<Schedule>of() : schedules.findByEmployeeIdAndEventDateBetweenOrderByEventDateAscStartTimeAsc(me.getId(),month.atDay(1),month.atEndOfMonth());
        model.addAttribute("schedules",monthSchedules);
        if(!admin && page.equals("schedule")) {
            var grouped=monthSchedules.stream().collect(java.util.stream.Collectors.groupingBy(Schedule::getEventDate));
            int offset=month.atDay(1).getDayOfWeek().getValue()%7;
            var start=month.atDay(1).minusDays(offset);
            int cells=((offset+month.lengthOfMonth()+6)/7)*7;
            var days=java.util.stream.IntStream.range(0,cells).mapToObj(i->{
                var day=start.plusDays(i);
                return new CalendarDay(day,YearMonth.from(day).equals(month),grouped.getOrDefault(day,List.of()));
            }).toList();
            model.addAttribute("calendarDays",days);
            model.addAttribute("calendarMonth",month);
            model.addAttribute("previousMonth",month.minusMonths(1).atDay(1));
            model.addAttribute("nextMonth",month.plusMonths(1).atDay(1));
            model.addAttribute("scheduleColors",Schedule.Color.values());
        }
        if(!admin && page.equals("attendance-status")) model.addAttribute("report",attendanceReport.report(me,date));
        if(!admin && (page.equals("overtime-request") || page.equals("leave-request") || page.equals("correction-request"))) {
            model.addAttribute("approvers",departments.approvers(me));
            model.addAttribute("actualOvertime",attendanceReport.report(me,today).overtime());
        }
        if(!admin && page.equals("request-status")) amendmentView.populate(model,me,false,type,requestId,amendmentId);
        if(admin) {
            var all=employees.findByDeletedAtIsNullOrderByNameAsc();
            model.addAttribute("employees",all.stream().filter(e->matches(e,query))
                .filter(e->departmentFilter.isBlank()||e.getDepartment().equals(departmentFilter))
                .filter(e->roleFilter.isBlank()||e.getRole().name().equals(roleFilter))
                .filter(e->activeFilter.isBlank()||Boolean.toString(e.isActive()).equals(activeFilter)).toList());
            model.addAttribute("departmentOptions",all.stream().map(Employee::getDepartment).distinct().sorted().toList());
            model.addAttribute("departmentFilter",departmentFilter);
            model.addAttribute("roleFilter",roleFilter);
            model.addAttribute("activeFilter",activeFilter);
            model.addAttribute("activeEmployees",all.stream().filter(Employee::isActive).toList());
            model.addAttribute("employeeCount",all.stream().filter(Employee::isActive).count());
            model.addAttribute("managerCount",all.stream().filter(departments::isManager).count());
        }
        return "portal";
    }
    private boolean matches(Employee e,String q) { return q.isBlank() || e.getName().contains(q) || e.getDepartment().contains(q) || e.getLoginId().contains(q); }
    private String base(Employee e) { return e.getRole()==Employee.Role.ADMIN ? "/admin" : "/employee"; }
    private String done(RedirectAttributes flash,String url) { flash.addFlashAttribute("success","처리되었습니다."); return "redirect:"+url; }

    @PostMapping("/employee/check-in") String checkIn(Principal p,RedirectAttributes f) { service.checkIn(p.getName()); return done(f,"/employee/main"); }
    @PostMapping("/employee/corrections") String correction(Principal p,@RequestParam Long approverId,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate workDate,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.TIME) LocalTime startTime,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.TIME) LocalTime endTime,@RequestParam String reason,RedirectAttributes f) {
        correctionService.request(p.getName(),approverId,workDate,startTime,endTime,reason);return done(f,"/employee/request-status");
    }
    @PostMapping("/employee/corrections/{id}/cancel") String cancelCorrection(Principal p,@PathVariable Long id,RedirectAttributes f){correctionService.cancel(p.getName(),id);return done(f,"/employee/request-status");}
    @PostMapping("/employee/check-out") String checkOut(Principal p,RedirectAttributes f) { service.checkOut(p.getName()); return done(f,"/employee/main"); }
    @PostMapping("/employee/leave") String leave(Principal p,@RequestParam Long approverId,@RequestParam LeaveRequest.Kind kind,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate startDate,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate endDate,@RequestParam String reason,RedirectAttributes f) {
        service.requestLeave(p.getName(),approverId,kind,startDate,endDate,reason); return done(f,"/employee/request-status");
    }
    @PostMapping("/employee/overtime") String overtime(Principal p,@RequestParam Long approverId,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate workDate,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.TIME) LocalTime startTime,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.TIME) LocalTime endTime,@RequestParam String reason,RedirectAttributes f) {
        overtimeService.request(p.getName(),approverId,workDate,startTime,endTime,reason); return done(f,"/employee/request-status");
    }
    @PostMapping("/employee/overtime/{id}/cancel") String cancelOvertime(Principal p,@PathVariable Long id,RedirectAttributes f) { overtimeService.cancel(p.getName(),id); return done(f,"/employee/request-status"); }
    @PostMapping("/employee/leave/{id}/cancel") String cancel(Principal p,@PathVariable Long id,RedirectAttributes f) { service.cancelLeave(p.getName(),id); return done(f,"/employee/request-status"); }
    @PostMapping("/employee/schedule") String schedule(Principal p,@RequestParam(required=false) Long employeeId,@RequestParam String title,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate eventDate,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.TIME) LocalTime startTime,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.TIME) LocalTime endTime,@RequestParam(defaultValue="") String memo,@RequestParam(defaultValue="BLUE") Schedule.Color color,RedirectAttributes f) {
        service.addSchedule(p.getName(),null,title,eventDate,startTime,endTime,memo,color); var e=service.current(p.getName()); return done(f,base(e)+(e.getRole()==Employee.Role.ADMIN ? "/schedule-assign" : "/schedule")+"?date="+eventDate);
    }
    @PostMapping("/employee/schedule/{id}/delete") String deleteSchedule(Principal p,@PathVariable Long id,RedirectAttributes f) { var eventDate=service.deleteSchedule(p.getName(),id); var e=service.current(p.getName()); return done(f,base(e)+(e.getRole()==Employee.Role.ADMIN ? "/schedule-assign" : "/schedule")+"?date="+eventDate); }
    @PostMapping("/employee/schedule/{id}/edit") String editSchedule(Principal p,@PathVariable Long id,@RequestParam String title,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate eventDate,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.TIME) LocalTime startTime,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.TIME) LocalTime endTime,@RequestParam(defaultValue="") String memo,@RequestParam Schedule.Color color,RedirectAttributes f) {
        service.updateSchedule(p.getName(),id,title,eventDate,startTime,endTime,memo,color); return done(f,"/employee/schedule?date="+eventDate);
    }
    @PostMapping("/admin/employees") String createEmployee(@RequestParam String loginId,@RequestParam String password,@RequestParam String name,@RequestParam String department,@RequestParam String position,@RequestParam Employee.Role role,@RequestParam(defaultValue="") String email,@RequestParam(defaultValue="") String phone,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate hireDate,@RequestParam(defaultValue="false") boolean departmentManager,RedirectAttributes f) {
        service.createEmployee(loginId,password,name,department,position,role,email,phone,hireDate,departmentManager); return done(f,"/admin/employee-manage");
    }
    @PostMapping("/admin/employees/{id}") String updateEmployee(Principal p,@PathVariable Long id,@RequestParam String name,@RequestParam String department,@RequestParam String position,@RequestParam boolean active,@RequestParam int annualDays,@RequestParam(defaultValue="false") boolean departmentManager,RedirectAttributes f) { service.updateEmployee(p.getName(),id,name,department,position,active,annualDays,departmentManager); return done(f,"/admin/employee-manage"); }
    @PostMapping("/admin/employees/{id}/delete") String deleteEmployee(Principal p,@PathVariable Long id,@RequestParam String confirmLoginId,RedirectAttributes f) {
        try {
            var deleted=service.deleteEmployee(p.getName(),id,confirmLoginId);
            for(var principal:sessionRegistry.getAllPrincipals()) {
                if(principal instanceof org.springframework.security.core.userdetails.UserDetails user && user.getUsername().equals(deleted))
                    sessionRegistry.getAllSessions(principal,false).forEach(org.springframework.security.core.session.SessionInformation::expireNow);
            }
            f.addFlashAttribute("success",deleted+" 계정을 삭제했습니다. 기존 업무 기록은 보존됩니다.");
        } catch(BusinessException ex) { f.addFlashAttribute("error",ex.getMessage()); }
        catch(org.springframework.dao.DataIntegrityViolationException ex) { f.addFlashAttribute("error","처리 중 데이터가 변경되었습니다. 새로고침 후 다시 확인해주세요."); }
        return "redirect:/admin/employee-manage";
    }
    @PostMapping("/employee/profile") String profile(Principal p,@RequestParam(defaultValue="") String email,@RequestParam(defaultValue="") String phone,@RequestParam(defaultValue="") String currentPassword,@RequestParam(defaultValue="") String newPassword,RedirectAttributes f) { service.updateProfile(p.getName(),email,phone,currentPassword,newPassword); return done(f,"/employee/mypage"); }
    @PostMapping("/admin/notices") String notice(Principal p,@RequestParam String title,@RequestParam String content,RedirectAttributes f) { service.publish(p.getName(),title,content); return done(f,"/admin/notification"); }
    @PostMapping({"/employee/notifications/{id}/read","/admin/notifications/{id}/read"}) String read(Principal p,@PathVariable Long id,RedirectAttributes f) { service.readNotification(p.getName(),id); var e=service.current(p.getName()); return done(f,base(e)+(e.getRole()==Employee.Role.ADMIN ? "/notification" : "/main")); }
}
