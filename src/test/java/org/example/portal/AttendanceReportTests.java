package org.example.portal;

import org.example.portal.domain.*;
import org.example.portal.repository.*;
import org.example.portal.service.AttendanceReportService;
import org.junit.jupiter.api.Test;
import java.time.*;
import java.util.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class AttendanceReportTests {
    final AttendanceRepository attendance=mock(AttendanceRepository.class);
    final LeaveRequestRepository leaves=mock(LeaveRequestRepository.class);
    final Clock clock=Clock.fixed(Instant.parse("2026-09-22T01:00:00Z"),ZoneId.of("Asia/Seoul"));
    final AttendanceReportService service=new AttendanceReportService(attendance,leaves,clock,
        new org.example.portal.service.WorkCalendarService(mock(CompanyHolidayRepository.class),mock(EmployeeRepository.class),leaves,clock));
    final Employee employee=new Employee();
    AttendanceReportTests() {
        employee.setHireDate(LocalDate.of(2026,9,14));
        when(attendance.findByEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(any(),any(),any())).thenReturn(List.of());
        when(leaves.findByEmployeeIdOrderByRequestedAtDesc(any())).thenReturn(List.of());
    }
    Attendance record(int day, String start, String end) {
        var r=new Attendance(); r.setWorkDate(LocalDate.of(2026,9,day)); r.setCheckIn(r.getWorkDate().atTime(LocalTime.parse(start)));
        if(end!=null) r.setCheckOut(r.getWorkDate().atTime(LocalTime.parse(end)));
        r.setLateArrival(LocalTime.parse(start).isAfter(LocalTime.of(9,0)));
        r.setEarlyDeparture(end!=null && LocalTime.parse(end).isBefore(LocalTime.of(18,0))); return r;
    }
    LeaveRequest leave(int day, LeaveRequest.Kind kind, LeaveRequest.Status status) {
        var r=new LeaveRequest(); r.setStartDate(LocalDate.of(2026,9,day)); r.setEndDate(r.getStartDate()); r.setKind(kind); r.setStatus(status); return r;
    }
    @Test void monthlyTotalsUseCompletedWorkAndApprovedLeave() {
        when(attendance.findByEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(any(),any(),any())).thenReturn(List.of(
            record(14,"09:00","18:00"),record(15,"09:30","18:00"),record(16,"09:00","16:00"),
            record(19,"08:00","19:00"),record(21,"14:00","18:00"),record(22,"09:00",null)));
        when(leaves.findByEmployeeIdOrderByRequestedAtDesc(any())).thenReturn(List.of(
            leave(17,LeaveRequest.Kind.ANNUAL,LeaveRequest.Status.PENDING),leave(18,LeaveRequest.Kind.ANNUAL,LeaveRequest.Status.APPROVED),
            leave(21,LeaveRequest.Kind.HALF_AM,LeaveRequest.Status.APPROVED),leave(23,LeaveRequest.Kind.SICK,LeaveRequest.Status.APPROVED)));
        var report=service.report(employee,LocalDate.of(2026,9,21));
        assertThat(report.workDays()).isEqualTo(6); assertThat(report.workTime()).isEqualTo("35시간 30분");
        assertThat(report.overtime()).isEqualTo("2시간 0분"); assertThat(report.lateDays()).isEqualTo(1); assertThat(report.absentDays()).isEqualTo(1);
        assertThat(report.days().get(1).badges()).extracting(AttendanceReportService.Badge::label).containsExactly("오전 반차");
        assertThat(report.days().get(2).badges()).extracting(AttendanceReportService.Badge::label).containsExactly("근무 중");
        assertThat(report.days().get(3).badges()).extracting(AttendanceReportService.Badge::label).containsExactly("병가");
        var past=service.report(employee,LocalDate.of(2026,9,14));
        assertThat(past.days().get(1).badges()).contains(new AttendanceReportService.Badge("정상","green"));
        assertThat(past.days().get(2).badges()).contains(new AttendanceReportService.Badge("지각","orange"));
        assertThat(past.days().get(3).badges()).contains(new AttendanceReportService.Badge("조퇴","blue"));
        assertThat(past.days().get(4).badges()).contains(new AttendanceReportService.Badge("결근","red"));
        assertThat(past.days().get(5).badges()).contains(new AttendanceReportService.Badge("연차","blue"));
    }
    @Test void partialLunchMissingCheckoutAndComplementaryHalfLeave() {
        when(attendance.findByEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(any(),any(),any())).thenReturn(List.of(
            record(14,"12:30","14:00"),record(15,"09:00",null),record(16,"09:00","14:00")));
        when(leaves.findByEmployeeIdOrderByRequestedAtDesc(any())).thenReturn(List.of(
            leave(16,LeaveRequest.Kind.HALF_PM,LeaveRequest.Status.APPROVED),leave(17,LeaveRequest.Kind.HALF_AM,LeaveRequest.Status.APPROVED),
            leave(18,LeaveRequest.Kind.HALF_AM,LeaveRequest.Status.APPROVED),leave(18,LeaveRequest.Kind.HALF_PM,LeaveRequest.Status.APPROVED)));
        var report=service.report(employee,LocalDate.of(2026,9,14));
        assertThat(report.workTime()).isEqualTo("5시간 0분");
        assertThat(report.days().get(1).badges()).extracting(AttendanceReportService.Badge::label).containsExactly("지각","조퇴");
        assertThat(report.days().get(2).badges()).extracting(AttendanceReportService.Badge::label).containsExactly("퇴근 미기록");
        assertThat(report.days().get(3).badges()).extracting(AttendanceReportService.Badge::label).containsExactly("오후 반차");
        assertThat(report.days().get(4).badges()).extracting(AttendanceReportService.Badge::label).containsExactly("오전 반차","결근");
        assertThat(report.days().get(5).badges()).extracting(AttendanceReportService.Badge::label).containsExactly("오전 반차","오후 반차");
    }
    @Test void weeksStayWithinMonthAndExcludeNonWorkingDates() {
        var first=service.report(employee,LocalDate.of(2026,9,1));
        assertThat(first.days()).hasSize(7); assertThat(first.days().get(0).currentMonth()).isFalse();
        assertThat(first.days().get(2).badges()).extracting(AttendanceReportService.Badge::label).containsExactly("입사 전");
        assertThat(first.previousWeek()).isEqualTo(LocalDate.of(2026,8,29));
        var last=service.report(employee,LocalDate.of(2026,9,30));
        assertThat(last.nextWeek()).isEqualTo(LocalDate.of(2026,10,1));
        assertThat(last.days().get(4).currentMonth()).isFalse();
        var year=service.report(employee,LocalDate.of(2027,1,1));
        assertThat(year.previousWeek()).isEqualTo(LocalDate.of(2026,12,26));
        assertThat(year.month()).isEqualTo(YearMonth.of(2026,9));
        assertThat(year.days().get(5).badges()).extracting(AttendanceReportService.Badge::label).containsExactly("예정");
    }
}
