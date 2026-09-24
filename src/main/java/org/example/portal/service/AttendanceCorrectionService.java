package org.example.portal.service;

import lombok.RequiredArgsConstructor;
import org.example.portal.domain.*;
import org.example.portal.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.Objects;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class AttendanceCorrectionService {
    private final PortalService portal;
    private final DepartmentAccess departments;
    private final EmployeeRepository employees;
    private final AttendanceRepository attendance;
    private final AttendanceCorrectionRepository corrections;
    private final NotificationRepository notifications;
    private final Clock clock;

    @Transactional public void request(String login,Long approverId,LocalDate date,LocalTime start,LocalTime end,String reason) {
        var e=employees.lockById(portal.current(login).getId()).orElseThrow();
        require(!e.isDeleted() && e.isActive() && e.getRole()==Employee.Role.EMPLOYEE,"재직 직원만 신청할 수 있습니다.");
        require(date!=null && !date.isAfter(portal.today()),"오늘 또는 과거 근무일을 선택하세요.");
        require(e.getHireDate()==null || !date.isBefore(e.getHireDate()),"입사일 이전 기록은 신청할 수 없습니다.");
        require(start!=null && end!=null && end.isAfter(start),"같은 날의 출근·퇴근 시간을 순서대로 입력하세요.");
        require(start.getSecond()==0 && end.getSecond()==0 && start.getNano()==0 && end.getNano()==0,"시간은 분 단위로 입력하세요.");
        require(!date.atTime(end).isAfter(LocalDateTime.now(clock)),"아직 지나지 않은 시간은 신청할 수 없습니다.");
        require(approverId!=null,"결재자를 선택하세요.");
        var approver=employees.findById(approverId).orElseThrow(()->new BusinessException("결재자가 없습니다."));
        require(departments.eligibleApprover(approver,e),"본인을 제외한 소속 부서 관리자를 선택하세요.");
        require(!corrections.existsByEmployeeIdAndWorkDateAndStatus(e.getId(),date,AttendanceCorrection.Status.PENDING),"해당 날짜에 승인 대기 중인 정정 신청이 있습니다.");
        var original=attendance.findByEmployeeIdAndWorkDate(e.getId(),date).orElse(null);
        require(original==null || !Objects.equals(original.getCheckIn(),date.atTime(start)) || !Objects.equals(original.getCheckOut(),date.atTime(end)),"기존 기록과 동일한 시간입니다.");
        var r=new AttendanceCorrection(); r.setEmployee(e); r.setApprover(approver); r.setWorkDate(date);
        if(original!=null){r.setOriginalVersion(original.getVersion());r.setOriginalCheckIn(original.getCheckIn());r.setOriginalCheckOut(original.getCheckOut());}
        r.setProposedCheckIn(date.atTime(start)); r.setProposedCheckOut(date.atTime(end));
        r.setReason(text(reason,1000,true)); r.setRequestedAt(LocalDateTime.now(clock)); corrections.save(r);
        notify(approver,e.getName()+" 님이 출퇴근 정정을 신청했습니다. ("+date+")");
    }
    @Transactional public void cancel(String login,Long id) {
        var e=employees.lockById(portal.current(login).getId()).orElseThrow();
        require(e.isActive() && !e.isDeleted(),"사용할 수 없는 계정입니다.");
        var r=corrections.lockById(id).orElseThrow(()->new BusinessException("신청 내역이 없습니다."));
        require(r.getEmployee().getId().equals(e.getId()),"본인 신청만 취소할 수 있습니다.");
        require(r.getStatus()==AttendanceCorrection.Status.PENDING,"승인 대기 중인 신청만 취소할 수 있습니다.");
        r.setStatus(AttendanceCorrection.Status.CANCELLED);
    }
    @Transactional public void review(String login,Long id,boolean approve,String comment) {
        var manager=departments.manager(login);
        var request=corrections.findById(id).orElseThrow(()->new BusinessException("신청 내역이 없습니다."));
        // Match check-in/out and deletion lock order: employee first, then request.
        var employee=employees.lockById(request.getEmployee().getId()).orElseThrow();
        var r=corrections.lockById(id).orElseThrow();
        departments.requireReview(manager,employee,r.getApprover());
        require(employee.isActive() && !employee.isDeleted(),"퇴사·비활성 직원의 신청은 승인할 수 없습니다.");
        require(r.getStatus()==AttendanceCorrection.Status.PENDING,"이미 처리된 신청입니다.");
        String review=text(comment,500,!approve);
        if(approve) {
            var a=attendance.findByEmployeeIdAndWorkDate(employee.getId(),r.getWorkDate()).orElse(null);
            require(a==null ? r.getOriginalVersion()==null : r.getOriginalVersion()!=null && a.getVersion()==r.getOriginalVersion()
                && Objects.equals(a.getCheckIn(),r.getOriginalCheckIn()) && Objects.equals(a.getCheckOut(),r.getOriginalCheckOut()),
                "신청 이후 출퇴근 기록이 변경되었습니다. 반려 후 다시 신청해주세요.");
            if(a==null){a=new Attendance();a.setEmployee(employee);a.setWorkDate(r.getWorkDate());}
            a.setCheckIn(r.getProposedCheckIn());a.setCheckOut(r.getProposedCheckOut());
            a.setLateArrival(a.getCheckIn().toLocalTime().isAfter(LocalTime.of(9,0)));
            a.setEarlyDeparture(a.getCheckOut().toLocalTime().isBefore(LocalTime.of(18,0))); attendance.save(a);
        }
        r.setStatus(approve ? AttendanceCorrection.Status.APPROVED : AttendanceCorrection.Status.REJECTED);
        r.setReviewer(manager);r.setReviewedAt(LocalDateTime.now(clock));r.setReviewComment(review);
        notify(employee,"출퇴근 정정 신청이 "+r.getStatus().getLabel()+"되었습니다. ("+r.getWorkDate()+")");
    }
    private String text(String value,int max,boolean required){value=value==null ? "" : value.trim();require(!required || !value.isBlank(),"사유를 입력하세요.");require(value.length()<=max,"입력 가능한 길이를 초과했습니다.");return value;}
    private void require(boolean ok,String message){if(!ok)throw new BusinessException(message);}
    private void notify(Employee e,String message){var n=new Notification();n.setRecipient(e);n.setMessage(message);n.setCreatedAt(LocalDateTime.now(clock));notifications.save(n);}
}
