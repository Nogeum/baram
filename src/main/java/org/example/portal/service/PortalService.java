package org.example.portal.service;

import lombok.RequiredArgsConstructor;
import org.example.portal.domain.*;
import org.example.portal.repository.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class PortalService {
    private final EmployeeRepository employees;
    private final AttendanceRepository attendance;
    private final LeaveRequestRepository leaves;
    private final OvertimeRequestRepository overtimeRequests;
    private final AttendanceCorrectionRepository corrections;
    private final ScheduleRepository schedules;
    private final NoticeRepository notices;
    private final NotificationRepository notifications;
    private final PasswordEncoder encoder;
    private final Clock clock;
    private final DepartmentAccess departments;
    private final WorkCalendarService workCalendar;

    public LocalDate today() { return LocalDate.now(clock); }
    public Employee current(String login) {
        var e = employees.findByLoginId(login).orElseThrow(() -> new BusinessException("계정이 없습니다."));
        require(!e.isDeleted() && e.isActive(), "삭제되었거나 비활성화된 계정입니다.");
        return e;
    }
    private Employee locked(String login) {
        var e = employees.lockById(current(login).getId()).orElseThrow();
        require(!e.isDeleted() && e.isActive(), "삭제되었거나 비활성화된 계정입니다.");
        return e;
    }
    public double remaining(Employee e, int year) {
        return (e.getAnnualUnits() - reserved(e.getId(), year)) / 2.0;
    }
    private int reserved(Long employeeId, int year) {
        return leaves.inYear(employeeId, LocalDate.of(year,1,1), LocalDate.of(year,12,31)).stream()
            .filter(r -> r.getStatus()==LeaveRequest.Status.PENDING || r.getStatus()==LeaveRequest.Status.APPROVED)
            .mapToInt(LeaveRequest::getChargedUnits).sum();
    }
    @Transactional public void checkIn(String login) {
        var e = locked(login);
        var now = LocalDateTime.now(clock);
        require(attendance.findByEmployeeIdAndWorkDate(e.getId(), now.toLocalDate()).isEmpty(), "이미 출근 처리되었습니다.");
        var a = new Attendance(); a.setEmployee(e); a.setWorkDate(now.toLocalDate()); a.setCheckIn(now);
        a.setLateArrival(now.toLocalTime().isAfter(LocalTime.of(9,0)));
        attendance.save(a);
    }
    @Transactional public void checkOut(String login) {
        var e = locked(login);
        var now = LocalDateTime.now(clock);
        var a = attendance.findByEmployeeIdAndWorkDate(e.getId(), now.toLocalDate()).orElseThrow(() -> new BusinessException("먼저 출근 체크를 해주세요."));
        require(a.getCheckOut()==null, "이미 퇴근 처리되었습니다.");
        a.setCheckOut(now); a.setEarlyDeparture(now.toLocalTime().isBefore(LocalTime.of(18,0)));
    }
    @Transactional public void requestLeave(String login, Long approverId, LeaveRequest.Kind kind, LocalDate start, LocalDate end, String reason) {
        var e = locked(login);
        require(approverId!=null,"결재자를 선택하세요.");
        var approver=employees.findById(approverId).orElseThrow(()->new BusinessException("결재자가 없습니다."));
        require(departments.eligibleApprover(approver,e),"본인을 제외한 소속 부서의 관리 권한자를 결재자로 선택하세요.");
        require(start!=null && end!=null && kind!=null, "휴가 종류와 날짜를 입력하세요.");
        require(!start.isBefore(today()) && !end.isBefore(start), "오늘 이후의 올바른 휴가 기간을 선택하세요.");
        require(start.getYear()==end.getYear(), "연도를 넘기는 휴가는 연도별로 나누어 신청하세요.");
        int days = workCalendar.days(start,end);
        require(days>0, "평일이 포함된 기간을 선택하세요.");
        boolean half = kind==LeaveRequest.Kind.HALF_AM || kind==LeaveRequest.Kind.HALF_PM;
        require(!half || start.equals(end), "반차는 하루만 선택하세요.");
        for (var r : leaves.inYear(e.getId(), LocalDate.of(start.getYear(),1,1), LocalDate.of(start.getYear(),12,31))) {
            if (r.getStatus()!=LeaveRequest.Status.PENDING && r.getStatus()!=LeaveRequest.Status.APPROVED) continue;
            boolean overlap = !r.getEndDate().isBefore(start) && !r.getStartDate().isAfter(end);
            boolean complementary = half && r.getStartDate().equals(start) && r.getEndDate().equals(end)
                && ((kind==LeaveRequest.Kind.HALF_AM && r.getKind()==LeaveRequest.Kind.HALF_PM) || (kind==LeaveRequest.Kind.HALF_PM && r.getKind()==LeaveRequest.Kind.HALF_AM));
            require(!overlap || complementary, "이미 신청하거나 승인된 휴가와 기간이 겹칩니다.");
        }
        int units = kind==LeaveRequest.Kind.SICK ? 0 : half ? 1 : days*2;
        require(reserved(e.getId(),start.getYear())+units <= e.getAnnualUnits(), "잔여 연차가 부족합니다.");
        var r = new LeaveRequest(); r.setEmployee(e); r.setApprover(approver); r.setKind(kind); r.setStartDate(start); r.setEndDate(end);
        r.setReason(required(reason,1000,"신청 사유")); r.setChargedUnits(units); r.setRequestedAt(LocalDateTime.now(clock)); leaves.save(r);
        notify(approver,e.getName()+" 님이 휴가를 신청했습니다.");
    }
    @Transactional public void cancelLeave(String login, Long id) {
        var e = locked(login);
        var r = leaves.lockById(id).orElseThrow(() -> new BusinessException("신청 내역이 없습니다."));
        require(r.getEmployee().getId().equals(e.getId()), "본인의 휴가만 취소할 수 있습니다.");
        require(r.getStatus()==LeaveRequest.Status.PENDING, "승인 대기 중인 신청만 취소할 수 있습니다.");
        r.setStatus(LeaveRequest.Status.CANCELLED);
    }
    @Transactional public void review(String login, Long id, boolean approve, String comment) {
        var admin = departments.manager(login);
        var r = leaves.lockById(id).orElseThrow(() -> new BusinessException("신청 내역이 없습니다."));
        departments.requireReview(admin,r.getEmployee(),r.getApprover());
        require(r.getStatus()==LeaveRequest.Status.PENDING, "이미 처리된 신청입니다.");
        r.setReviewComment(approve ? optional(comment,500,"처리 의견") : required(comment,500,"반려 사유"));
        r.setStatus(approve ? LeaveRequest.Status.APPROVED : LeaveRequest.Status.REJECTED);
        r.setReviewer(admin); r.setReviewedAt(LocalDateTime.now(clock));
        r.setApprover(admin);
        notify(r.getEmployee(), "휴가 신청이 " + r.getStatus().getLabel() + "되었습니다. ("+r.getStartDate()+")");
    }
    @Transactional public void addSchedule(String login, Long employeeId, String title, LocalDate date, LocalTime start, LocalTime end, String memo, Schedule.Color color) {
        var actor = current(login);
        require(actor.getRole()==Employee.Role.EMPLOYEE,"직원 계정만 일정을 등록할 수 있습니다.");
        boolean assigned = employeeId!=null;
        var e = assigned ? employees.findById(employeeId).orElseThrow(() -> new BusinessException("직원이 없습니다.")) : actor;
        if(assigned) departments.requireMember(departments.manager(login),e);
        require(!e.isDeleted() && e.isActive(), "삭제되었거나 비활성인 직원에게 배정할 수 없습니다.");
        require(date!=null, "날짜를 선택하세요.");
        require((start==null && end==null) || (start!=null && end!=null && end.isAfter(start)), "시작·종료 시간을 올바르게 입력하세요. 야간 일정은 날짜별로 나누어 등록하세요.");
        var s = new Schedule(); s.setEmployee(e); s.setTitle(required(title,120,"일정명")); s.setEventDate(date);
        s.setStartTime(start); s.setEndTime(end); s.setMemo(optional(memo,500,"메모")); s.setAssigned(assigned); s.setColor(Objects.requireNonNull(color)); schedules.save(s);
        if(assigned) notify(e,"새 일정이 배정되었습니다: "+s.getTitle());
    }
    @Transactional public void updateSchedule(String login, Long id, String title, LocalDate date, LocalTime start, LocalTime end, String memo, Schedule.Color color) {
        var actor=current(login); var s=schedules.findById(id).orElseThrow(() -> new BusinessException("일정이 없습니다."));
        require(!s.isAssigned() && s.getEmployee().getId().equals(actor.getId()), "본인의 개인 일정만 수정할 수 있습니다.");
        require(date!=null, "날짜를 선택하세요.");
        require((start==null && end==null) || (start!=null && end!=null && end.isAfter(start)), "시작·종료 시간을 올바르게 입력하세요. 야간 일정은 날짜별로 나누어 등록하세요.");
        var validTitle=required(title,120,"일정명"); var validMemo=optional(memo,500,"메모");
        s.setTitle(validTitle); s.setEventDate(date); s.setStartTime(start); s.setEndTime(end); s.setMemo(validMemo); s.setColor(Objects.requireNonNull(color));
    }
    @Transactional public LocalDate deleteSchedule(String login, Long id) {
        var e = current(login); var s = schedules.findById(id).orElseThrow(() -> new BusinessException("일정이 없습니다."));
        require(!s.isAssigned() && s.getEmployee().getId().equals(e.getId()), "본인의 개인 일정만 삭제할 수 있습니다.");
        schedules.delete(s);
        return s.getEventDate();
    }
    @Transactional public LocalDate deleteAssignedSchedule(String login, Long id) {
        var manager=departments.manager(login); var s=schedules.findById(id).orElseThrow(()->new BusinessException("일정이 없습니다."));
        departments.requireMember(manager,s.getEmployee()); require(s.isAssigned(),"부서 배정 일정만 삭제할 수 있습니다.");
        schedules.delete(s); return s.getEventDate();
    }
    @Transactional public Employee createEmployee(String login, String password, String name, String department, String position, Employee.Role role, String email, String phone, LocalDate hireDate, boolean departmentManager) {
        require(!departmentManager || role==Employee.Role.EMPLOYEE,"부서 관리 권한은 직원 계정에만 부여할 수 있습니다.");
        var e=createEmployee(login,password,name,department,position,role,email,phone,hireDate); e.setDepartmentManager(departmentManager); return e;
    }
    @Transactional public Employee createEmployee(String login, String password, String name, String department, String position, Employee.Role role, String email, String phone, LocalDate hireDate) {
        login=required(login,60,"로그인 ID");
        require(login.matches("[A-Za-z0-9._-]{3,60}"),"로그인 ID는 영문·숫자·점·밑줄·하이픈 3~60자입니다.");
        require(employees.findByLoginId(login).isEmpty(),"이미 사용 중인 로그인 ID입니다.");
        validatePassword(password);
        var e = new Employee(); e.setLoginId(login); e.setPassword(encoder.encode(password));
        e.setName(required(name,80,"이름")); e.setDepartment(required(department,80,"부서")); e.setPositionName(required(position,40,"직급"));
        e.setRole(Objects.requireNonNull(role)); e.setEmail(email(email)); e.setPhone(optional(phone,30,"연락처")); e.setHireDate(hireDate);
        return employees.save(e);
    }
    @Transactional public void updateEmployee(String login, Long id, String name, String department, String position, boolean active, int annualDays) {
        var admin = current(login); require(admin.getRole()==Employee.Role.ADMIN,"관리자 권한이 필요합니다.");
        var e = employees.lockById(id).orElseThrow(() -> new BusinessException("직원이 없습니다."));
        require(!e.isDeleted(),"삭제된 계정은 수정하거나 다시 활성화할 수 없습니다.");
        require(e.getRole()==Employee.Role.EMPLOYEE || active,"관리자 계정은 비활성화할 수 없습니다.");
        require(annualDays>=0 && annualDays<=365, "연차는 0~365일 범위로 입력하세요.");
        int maxReserved=leaves.findByEmployeeIdOrderByRequestedAtDesc(id).stream()
            .map(r->r.getStartDate().getYear()).distinct().filter(y->y>=today().getYear()).mapToInt(y->reserved(id,y)).max().orElse(0);
        require(annualDays*2>=maxReserved,"신청·승인된 휴가보다 연차를 줄일 수 없습니다.");
        e.setName(required(name,80,"이름")); e.setDepartment(required(department,80,"부서")); e.setPositionName(required(position,40,"직급")); e.setActive(active); e.setAnnualUnits(annualDays*2);
    }
    @Transactional public void updateEmployee(String login, Long id, String name, String department, String position, boolean active, int annualDays, boolean departmentManager) {
        updateEmployee(login,id,name,department,position,active,annualDays);
        var e=employees.findById(id).orElseThrow();
        require(!departmentManager || e.getRole()==Employee.Role.EMPLOYEE,"부서 관리 권한은 직원 계정에만 부여할 수 있습니다.");
        e.setDepartmentManager(departmentManager);
    }
    @Transactional public String deleteEmployee(String login, Long id, String confirmLoginId) {
        var admins=employees.lockByRole(Employee.Role.ADMIN);
        var actor=current(login);
        if(actor.getRole()!=Employee.Role.ADMIN) throw new org.springframework.security.access.AccessDeniedException("웹 관리자만 계정을 삭제할 수 있습니다.");
        require(!actor.getId().equals(id),"로그인 중인 본인 계정은 삭제할 수 없습니다.");
        var target=employees.lockById(id).orElseThrow(()->new BusinessException("계정이 없거나 이미 삭제되었습니다."));
        require(!target.isDeleted(),"이미 삭제된 계정입니다.");
        require(target.getLoginId().equals(confirmLoginId),"삭제할 계정의 로그인 ID를 정확히 입력하세요.");
        require(target.getRole()!=Employee.Role.ADMIN || admins.stream().anyMatch(e->!e.isDeleted() && e.isActive() && !e.getId().equals(id)),"마지막 웹 관리자 계정은 삭제할 수 없습니다.");
        target.setDeletedAt(LocalDateTime.now(clock)); target.setDeletedByLogin(actor.getLoginId());
        target.setActive(false); target.setDepartmentManager(false); target.setPassword(encoder.encode(UUID.randomUUID().toString()));
        leaves.cancelPendingForDeletedEmployee(id,LeaveRequest.Status.PENDING,LeaveRequest.Status.CANCELLED);
        overtimeRequests.cancelPendingForDeletedEmployee(id,OvertimeRequest.Status.PENDING,OvertimeRequest.Status.CANCELLED);
        corrections.cancelForDeletedEmployee(id,AttendanceCorrection.Status.PENDING,AttendanceCorrection.Status.CANCELLED);
        employees.flush(); return target.getLoginId();
    }
    @Transactional public void updateProfile(String login, String email, String phone, String currentPassword, String newPassword) {
        var e = locked(login); e.setEmail(email(email)); e.setPhone(optional(phone,30,"연락처"));
        if(newPassword!=null && !newPassword.isBlank()) {
            require(currentPassword!=null && encoder.matches(currentPassword,e.getPassword()),"현재 비밀번호가 일치하지 않습니다.");
            validatePassword(newPassword); e.setPassword(encoder.encode(newPassword));
        }
    }
    @Transactional public void publish(String login, String title, String content) {
        var e=current(login); require(e.getRole()==Employee.Role.ADMIN,"관리자 권한이 필요합니다.");
        var n = new Notice(); n.setAuthor(e); n.setTitle(required(title,160,"제목")); n.setContent(required(content,2000,"내용")); n.setCreatedAt(LocalDateTime.now(clock)); notices.save(n);
    }
    @Transactional public void readNotification(String login, Long id) {
        var n = notifications.findById(id).orElseThrow(() -> new BusinessException("알림이 없습니다."));
        require(n.getRecipient().getId().equals(current(login).getId()),"본인 알림만 처리할 수 있습니다."); n.setReadFlag(true);
    }
    private void notify(Employee e, String message) {
        var n = new Notification(); n.setRecipient(e); n.setMessage(message); n.setCreatedAt(LocalDateTime.now(clock)); notifications.save(n);
    }
    private static String email(String value) {
        value=optional(value,120,"이메일"); require(value.isEmpty() || value.matches("[^\\s@]+@[^\\s@]+\\.[^\\s@]+"),"이메일 형식을 확인하세요."); return value;
    }
    private static void validatePassword(String p) { require(p!=null && p.length()>=8 && p.getBytes(java.nio.charset.StandardCharsets.UTF_8).length<=72,"비밀번호는 8자 이상, UTF-8 기준 72바이트 이하로 입력하세요."); }
    private static String required(String value, int max, String field) { value=optional(value,max,field); require(!value.isBlank(),field+"을(를) 입력하세요."); return value; }
    private static String optional(String value, int max, String field) { value=value==null ? "" : value.trim(); require(value.length()<=max,field+"은(는) "+max+"자 이하로 입력하세요."); return value; }
    private static void require(boolean condition, String message) { if(!condition) throw new BusinessException(message); }
}
