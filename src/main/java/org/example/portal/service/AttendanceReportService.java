package org.example.portal.service;

import lombok.RequiredArgsConstructor;
import org.example.portal.domain.*;
import org.example.portal.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class AttendanceReportService {
    private final AttendanceRepository attendance;
    private final LeaveRequestRepository leaves;
    private final Clock clock;
    private final WorkCalendarService workCalendar;

    public record Badge(String label, String tone) {}
    public record Day(LocalDate date, boolean currentMonth, boolean today, String times, String duration, List<Badge> badges) {}
    public record Report(YearMonth month, int workDays, String workTime, String overtime, int lateDays, int absentDays,
                         String weekLabel, LocalDate previousWeek, LocalDate nextWeek, List<Day> days) {}
    private record State(List<Badge> badges, boolean late, boolean absent) {}
    public record Monthly(int workDays,long workMinutes,long overtimeMinutes,int lateDays,int earlyDays,int absentDays,double leaveDays,int missingCheckouts) {
        public String workTime(){return duration(workMinutes);}
        public String overtime(){return duration(overtimeMinutes);}
    }
    public Monthly monthly(Employee e,YearMonth month,List<Attendance> records,List<LeaveRequest> approved) {
        var today=LocalDate.now(clock); var closed=workCalendar.closedDates();
        var byDay=records.stream().collect(Collectors.toMap(Attendance::getWorkDate,Function.identity()));
        int work=0,late=0,early=0,absent=0,missing=0;long minutes=0,extra=0;double leaveDays=0;
        for(var day=month.atDay(1);!day.isAfter(month.atEndOfMonth()) && !day.isAfter(today);day=day.plusDays(1)) {
            var a=byDay.get(day);var s=state(e,day,today,a,approved,closed);
            if(a!=null){work++;long m=workedMinutes(a);minutes+=m;extra+=Math.max(0,m-480);if(a.getCheckOut()==null)missing++;}
            if(s.late())late++;if(s.absent())absent++;
            if(s.badges().stream().anyMatch(b->b.label().equals("조퇴")))early++;
            if(workCalendar.workingDay(day,closed)) {
                final var d=day;
                double units=approved.stream().filter(r->!d.isBefore(r.getStartDate()) && !d.isAfter(r.getEndDate()))
                    .mapToDouble(r->r.getKind()==LeaveRequest.Kind.HALF_AM || r.getKind()==LeaveRequest.Kind.HALF_PM ? 0.5 : 1).sum();
                leaveDays+=Math.min(1,units);
            }
        }
        return new Monthly(work,minutes,extra,late,early,absent,leaveDays,missing);
    }

    public Report report(Employee employee, LocalDate selected) {
        var today=LocalDate.now(clock); var closed=workCalendar.closedDates();
        var month=YearMonth.from(today);
        var weekMonth=YearMonth.from(selected);
        var first=month.atDay(1).isBefore(weekMonth.atDay(1)) ? month.atDay(1) : weekMonth.atDay(1);
        var last=month.atEndOfMonth().isAfter(weekMonth.atEndOfMonth()) ? month.atEndOfMonth() : weekMonth.atEndOfMonth();
        var records=attendance.findByEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(employee.getId(),first,last).stream()
            .collect(Collectors.toMap(Attendance::getWorkDate,Function.identity()));
        var approved=leaves.findByEmployeeIdOrderByRequestedAtDesc(employee.getId()).stream()
            .filter(r->r.getStatus()==LeaveRequest.Status.APPROVED && !r.getEndDate().isBefore(first) && !r.getStartDate().isAfter(last)).toList();
        int workDays=0, late=0, absent=0; long minutes=0, overtime=0;
        for(var day=month.atDay(1); !day.isAfter(today); day=day.plusDays(1)) {
            var record=records.get(day);
            if(record!=null) {
                workDays++;
                long worked=workedMinutes(record);
                minutes+=worked; overtime+=Math.max(0,worked-480);
            }
            var state=state(employee,day,today,record,approved,closed);
            if(state.late()) late++;
            if(state.absent()) absent++;
        }
        var start=selected.minusDays(selected.getDayOfWeek().getValue()%7);
        var days=new ArrayList<Day>();
        for(int i=0;i<7;i++) {
            var day=start.plusDays(i); boolean inMonth=YearMonth.from(day).equals(weekMonth);
            var record=records.get(day);
            var state=inMonth ? state(employee,day,today,record,approved,closed) : new State(List.of(),false,false);
            days.add(new Day(day,inMonth,day.equals(today),record==null ? "" : time(record.getCheckIn())+" – "+time(record.getCheckOut()),
                record==null || record.getCheckOut()==null ? "" : duration(workedMinutes(record)),state.badges()));
        }
        var previous=start.minusDays(1);
        var next=start.plusDays(7).isAfter(weekMonth.atEndOfMonth()) ? weekMonth.plusMonths(1).atDay(1) : start.plusDays(7);
        int week=(weekMonth.atDay(1).getDayOfWeek().getValue()%7+selected.getDayOfMonth()-1)/7+1;
        return new Report(month,workDays,duration(minutes),duration(overtime),late,absent,
            weekMonth.getYear()+"년 "+weekMonth.getMonthValue()+"월 "+week+"주",previous,next,List.copyOf(days));
    }

    private State state(Employee employee, LocalDate day, LocalDate today, Attendance record, List<LeaveRequest> approved, Set<LocalDate> closed) {
        boolean weekday=workCalendar.workingDay(day,closed);
        var kinds=EnumSet.noneOf(LeaveRequest.Kind.class);
        if(weekday) approved.stream().filter(r->!day.isBefore(r.getStartDate()) && !day.isAfter(r.getEndDate())).forEach(r->kinds.add(r.getKind()));
        var badges=new ArrayList<Badge>();
        kinds.forEach(k->badges.add(new Badge(k.getLabel(),"blue")));
        boolean full=kinds.contains(LeaveRequest.Kind.ANNUAL) || kinds.contains(LeaveRequest.Kind.SICK)
            || (kinds.contains(LeaveRequest.Kind.HALF_AM) && kinds.contains(LeaveRequest.Kind.HALF_PM));
        boolean late=false, absent=false;
        if(record!=null) {
            late=weekday && record.isLateArrival() && !full && !kinds.contains(LeaveRequest.Kind.HALF_AM);
            boolean early=weekday && record.isEarlyDeparture() && !full && !kinds.contains(LeaveRequest.Kind.HALF_PM);
            if(late) badges.add(new Badge("지각","orange"));
            if(early) badges.add(new Badge("조퇴","blue"));
            if(record.getCheckOut()==null) badges.add(new Badge(day.equals(today) ? "근무 중" : "퇴근 미기록","neutral"));
            else if(!late && !early && kinds.isEmpty()) badges.add(new Badge("정상","green"));
        } else if(employee.getHireDate()!=null && day.isBefore(employee.getHireDate())) {
            if(badges.isEmpty()) badges.add(new Badge("입사 전","neutral"));
        } else if(employee.isDeleted() && day.isAfter(employee.getDeletedAt().toLocalDate())) {
            if(badges.isEmpty()) badges.add(new Badge("퇴사 후","neutral"));
        } else if(weekday && day.isBefore(today) && !full) {
            absent=true; badges.add(new Badge("결근","red"));
        } else if(badges.isEmpty()) {
            badges.add(new Badge(!weekday ? "휴무" : day.equals(today) ? "출근 전" : "예정","neutral"));
        }
        return new State(List.copyOf(badges),late,absent);
    }

    private long workedMinutes(Attendance record) {
        if(record.getCheckOut()==null || !record.getCheckOut().isAfter(record.getCheckIn())) return 0;
        var start=record.getCheckIn(); var end=record.getCheckOut();
        long seconds=Duration.between(start,end).getSeconds();
        for(var day=start.toLocalDate(); !day.isAfter(end.toLocalDate()); day=day.plusDays(1)) {
            var lunchStart=day.atTime(12,0); var lunchEnd=day.atTime(13,0);
            var overlapStart=start.isAfter(lunchStart) ? start : lunchStart;
            var overlapEnd=end.isBefore(lunchEnd) ? end : lunchEnd;
            if(overlapEnd.isAfter(overlapStart)) seconds-=Duration.between(overlapStart,overlapEnd).getSeconds();
        }
        return Math.max(0,seconds/60);
    }
    private static String duration(long minutes) { return minutes/60+"시간 "+minutes%60+"분"; }
    private String time(LocalDateTime time) { return time==null ? "--:--" : time.format(DateTimeFormatter.ofPattern("HH:mm")); }
}
