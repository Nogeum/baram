package org.example.portal.service;

import lombok.RequiredArgsConstructor;
import org.example.portal.domain.*;
import org.example.portal.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class OvertimeService {
    private final PortalService portal;
    private final EmployeeRepository employees;
    private final OvertimeRequestRepository requests;
    private final NotificationRepository notifications;
    private final Clock clock;
    private final DepartmentAccess departments;

    @Transactional public void request(String login, Long approverId, LocalDate date, LocalTime start, LocalTime end, String reason) {
        var employee=employees.lockById(portal.current(login).getId()).orElseThrow();
        require(employee.isActive() && employee.getRole()==Employee.Role.EMPLOYEE,"직원 계정만 신청할 수 있습니다.");
        var now=LocalDateTime.now(clock);
        require(date!=null && !date.isAfter(now.toLocalDate()),"오늘까지의 근무일자를 선택하세요. 미래 근무는 신청할 수 없습니다.");
        require(employee.getHireDate()==null || !date.isBefore(employee.getHireDate()),"입사일 이후의 근무일자를 선택하세요.");
        require(start!=null && end!=null && end.isAfter(start) && Duration.between(start,end).toMinutes()>0,"종료 시각은 시작 시각 이후여야 합니다. 자정을 넘는 근무는 날짜별로 나누어 신청하세요.");
        require(start.getSecond()==0 && start.getNano()==0 && end.getSecond()==0 && end.getNano()==0,"시각은 분 단위로 입력하세요.");
        require(!date.atTime(end).isAfter(now),"이미 종료된 초과근무만 신청할 수 있습니다.");
        require(approverId!=null,"결재자를 선택하세요.");
        var approver=employees.findById(approverId).orElseThrow(()->new BusinessException("결재자가 없습니다."));
        require(departments.eligibleApprover(approver,employee),"본인을 제외한 소속 부서의 관리 권한자를 결재자로 선택하세요.");
        reason=text(reason,1000,"신청 사유",true);
        for(var previous:requests.findByEmployeeIdAndWorkDate(employee.getId(),date)) {
            if(previous.getStatus()!=OvertimeRequest.Status.PENDING && previous.getStatus()!=OvertimeRequest.Status.APPROVED) continue;
            require(!start.isBefore(previous.getEndTime()) || !end.isAfter(previous.getStartTime()),"이미 신청하거나 승인된 초과근무 시간과 겹칩니다.");
        }
        var r=new OvertimeRequest(); r.setEmployee(employee); r.setApprover(approver); r.setWorkDate(date); r.setStartTime(start); r.setEndTime(end);
        r.setReason(reason); r.setRequestedAt(LocalDateTime.now(clock)); requests.save(r);
        notify(approver,employee.getName()+" 님이 초과근무를 신청했습니다. ("+date+")");
    }
    @Transactional public void cancel(String login, Long id) {
        var actor=portal.current(login); var r=requests.lockById(id).orElseThrow(()->new BusinessException("신청 내역이 없습니다."));
        require(r.getEmployee().getId().equals(actor.getId()),"본인의 신청만 취소할 수 있습니다.");
        require(r.getStatus()==OvertimeRequest.Status.PENDING,"승인 대기 중인 신청만 취소할 수 있습니다.");
        r.setStatus(OvertimeRequest.Status.CANCELLED);
    }
    @Transactional public void review(String login, Long id, boolean approve, String comment) {
        var actor=departments.manager(login); var r=requests.lockById(id).orElseThrow(()->new BusinessException("신청 내역이 없습니다."));
        departments.requireReview(actor,r.getEmployee(),r.getApprover());
        require(r.getStatus()==OvertimeRequest.Status.PENDING,"이미 처리된 신청입니다.");
        r.setReviewComment(text(comment,500,"반려 사유",!approve));
        r.setStatus(approve ? OvertimeRequest.Status.APPROVED : OvertimeRequest.Status.REJECTED); r.setReviewedAt(LocalDateTime.now(clock));
        r.setApprover(actor);
        notify(r.getEmployee(),"초과근무 신청이 "+r.getStatus().getLabel()+"되었습니다. ("+r.getWorkDate()+")");
    }
    private void notify(Employee employee,String message) { var n=new Notification(); n.setRecipient(employee); n.setMessage(message); n.setCreatedAt(LocalDateTime.now(clock)); notifications.save(n); }
    private String text(String value,int max,String field,boolean required) { value=value==null ? "" : value.trim(); require(!required || !value.isBlank(),field+"을 입력하세요."); require(value.length()<=max,field+"은 "+max+"자 이하로 입력하세요."); return value; }
    private void require(boolean condition,String message) { if(!condition) throw new BusinessException(message); }
}
