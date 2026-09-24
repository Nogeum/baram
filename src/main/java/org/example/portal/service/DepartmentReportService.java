package org.example.portal.service;

import lombok.RequiredArgsConstructor;
import org.example.portal.domain.*;
import org.example.portal.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class DepartmentReportService {
    private final DepartmentAccess access;
    private final EmployeeRepository employees;
    private final AttendanceRepository attendance;
    private final LeaveRequestRepository leaves;
    private final WorkTaskRepository tasks;
    private final AttendanceReportService calculator;
    public record Row(Employee employee,AttendanceReportService.Monthly totals) {}
    public record Monthly(YearMonth month,String department,List<Row> rows) {}
    public record LeaveItem(String name,String kind) {}
    public record TaskItem(Long id,String name,String title,String status,String statusLabel,boolean own) {}
    public record CalendarDay(LocalDate date,boolean currentMonth,List<LeaveItem> items,List<TaskItem> tasks) {}
    public record Calendar(YearMonth month,String department,List<CalendarDay> days,int requestCount,int taskCount) {}
    public Monthly monthly(String login,YearMonth month,String query) {
        var manager=access.manager(login);
        var records=attendance.findByEmployeeDepartmentAndEmployeeRoleAndWorkDateBetweenOrderByWorkDateAsc(manager.getDepartment(),Employee.Role.EMPLOYEE,month.atDay(1),month.atEndOfMonth())
            .stream().collect(Collectors.groupingBy(a->a.getEmployee().getId()));
        var approved=approved(manager,month.atDay(1),month.atEndOfMonth()).stream().collect(Collectors.groupingBy(r->r.getEmployee().getId()));
        var rows=employees.findByDepartmentAndRoleOrderByNameAsc(manager.getDepartment(),Employee.Role.EMPLOYEE).stream()
            .filter(e->e.getHireDate()==null || !e.getHireDate().isAfter(month.atEndOfMonth()))
            .filter(e->!e.isDeleted() || !e.getDeletedAt().toLocalDate().isBefore(month.atDay(1)) || records.containsKey(e.getId()) || approved.containsKey(e.getId()))
            .filter(e->query==null || query.isBlank() || e.getName().contains(query) || e.getLoginId().contains(query))
            .map(e->new Row(e,calculator.monthly(e,month,records.getOrDefault(e.getId(),List.of()),approved.getOrDefault(e.getId(),List.of())))).toList();
        return new Monthly(month,manager.getDepartment(),rows);
    }
    public Calendar calendar(String login,YearMonth month) {
        var manager=access.manager(login);
        var start=month.atDay(1).minusDays(month.atDay(1).getDayOfWeek().getValue()%7);
        int cells=((month.atDay(1).getDayOfWeek().getValue()%7+month.lengthOfMonth()+6)/7)*7;
        var approved=approved(manager,month.atDay(1),month.atEndOfMonth());
        var departmentTasks=tasks.departmentCalendar(manager.getDepartment(),Employee.Role.EMPLOYEE,month.atDay(1),month.atEndOfMonth());
        var byDate=departmentTasks.stream().collect(Collectors.groupingBy(WorkTask::getDueDate));
        var days=new ArrayList<CalendarDay>();
        for(int i=0;i<cells;i++) {
            var day=start.plusDays(i);boolean inMonth=YearMonth.from(day).equals(month);
            var items=!inMonth || day.getDayOfWeek()==DayOfWeek.SUNDAY || day.getDayOfWeek()==DayOfWeek.SATURDAY ? List.<LeaveItem>of() :
                approved.stream().filter(r->!day.isBefore(r.getStartDate()) && !day.isAfter(r.getEndDate()))
                    .map(r->new LeaveItem(r.getEmployee().getDisplayName(),r.getKind().getLabel())).toList();
            var taskItems=byDate.getOrDefault(day,List.of()).stream()
                .map(t->new TaskItem(t.getId(),t.getAssignee().getDisplayName(),t.getTitle(),t.getStatus(),t.getStatusLabel(),t.getAssignee().getId().equals(manager.getId()))).toList();
            days.add(new CalendarDay(day,inMonth,items,taskItems));
        }
        return new Calendar(month,manager.getDepartment(),List.copyOf(days),approved.size(),departmentTasks.size());
    }
    private List<LeaveRequest> approved(Employee manager,LocalDate start,LocalDate end){return leaves.departmentApproved(manager.getDepartment(),Employee.Role.EMPLOYEE,LeaveRequest.Status.APPROVED,start,end);}
}
