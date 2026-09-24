package org.example.portal.service;
import lombok.RequiredArgsConstructor;
import org.example.portal.domain.*;
import org.example.portal.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.*;

@Service @RequiredArgsConstructor @Transactional(readOnly=true)
public class AmendmentService {
    private final PortalService portal;
    private final DepartmentAccess departments;
    private final GroupwareService groupware;
    private final OvertimeService overtime;
    private final AttendanceCorrectionService correctionService;
    private final EmployeeRepository employees;
    private final LeaveRequestRepository leaves;
    private final OvertimeRequestRepository overtimes;
    private final AttendanceCorrectionRepository corrections;
    private final AttendanceRepository attendance;
    private final RequestAmendmentRepository amendments;
    private final Clock clock;
    public record Original(String type,Long id,Employee employee,long version,String summary,LocalDate startDate,LocalDate endDate,String kind,LocalTime startTime,LocalTime endTime) {}
    private void require(boolean ok,String message){if(!ok)throw new BusinessException(message);}
    private String text(String v,int max){v=v==null?"":v.trim();require(!v.isBlank()&&v.length()<=max,"사유를 "+max+"자 이내로 입력하세요.");return v;}
    public Original original(String type,Long id){
        return switch(type){
            case "LEAVE"->{var r=leaves.findById(id).orElseThrow(()->new BusinessException("휴가 신청이 없습니다."));require(r.getStatus()==LeaveRequest.Status.APPROVED,"승인된 신청만 취소·변경 요청할 수 있습니다.");yield new Original(type,id,r.getEmployee(),r.getVersion(),r.getKind().getLabel()+" "+r.getStartDate()+" ~ "+r.getEndDate(),r.getStartDate(),r.getEndDate(),r.getKind().name(),null,null);}
            case "OVERTIME"->{var r=overtimes.findById(id).orElseThrow(()->new BusinessException("초과근무 신청이 없습니다."));require(r.getStatus()==OvertimeRequest.Status.APPROVED,"승인된 신청만 취소·변경 요청할 수 있습니다.");yield new Original(type,id,r.getEmployee(),r.getVersion(),r.getWorkDate()+" "+r.getStartTime()+" ~ "+r.getEndTime(),r.getWorkDate(),r.getWorkDate(),null,r.getStartTime(),r.getEndTime());}
            case "CORRECTION"->{var r=corrections.findById(id).orElseThrow(()->new BusinessException("정정 신청이 없습니다."));require(r.getStatus()==AttendanceCorrection.Status.APPROVED,"승인된 신청만 취소·변경 요청할 수 있습니다.");yield new Original(type,id,r.getEmployee(),r.getVersion(),r.getWorkDate()+" "+r.getProposedCheckIn().toLocalTime()+" ~ "+r.getProposedCheckOut().toLocalTime(),r.getWorkDate(),r.getWorkDate(),null,r.getProposedCheckIn().toLocalTime(),r.getProposedCheckOut().toLocalTime());}
            default->throw new BusinessException("신청 종류가 올바르지 않습니다.");
        };
    }
    public Original ownOriginal(Employee a,String type,Long id){var o=original(type,id);require(o.employee().getId().equals(a.getId()),"본인의 신청만 변경할 수 있습니다.");return o;}
    public boolean canReview(Employee a,RequestAmendment r){return r.getStatus().equals("PENDING")&&departments.canReview(a,r.getEmployee(),r.getApprover());}
    public List<RequestAmendment> list(Employee a){return amendments.findAll().stream().filter(r->r.getEmployee().getId().equals(a.getId())||departments.canReview(a,r.getEmployee(),r.getApprover())).sorted(Comparator.comparing(RequestAmendment::getCreatedAt).reversed()).toList();}
    public RequestAmendment detail(Employee a,Long id){var r=amendments.findById(id).orElseThrow(()->new BusinessException("변경 요청이 없습니다."));require(r.getEmployee().getId().equals(a.getId())||departments.canReview(a,r.getEmployee(),r.getApprover()),"조회 권한이 없습니다.");return r;}
    @Transactional public Long request(String login,String type,Long id,String action,Long approverId,String reason,LocalDate start,LocalDate end,String kind,LocalTime startTime,LocalTime endTime){
        var a=employees.lockById(portal.current(login).getId()).orElseThrow();var o=ownOriginal(a,type,id);
        require(Set.of("CANCEL","CHANGE").contains(action),"요청 종류를 선택하세요.");
        require(amendments.findAll().stream().noneMatch(r->r.getRequestType().equals(type)&&r.getRequestId().equals(id)&&r.getStatus().equals("PENDING")),"이미 승인 대기 중인 취소·변경 요청이 있습니다.");
        var approver=employees.findById(approverId).orElseThrow(()->new BusinessException("결재자가 없습니다."));require(departments.eligibleApprover(approver,a),"같은 부서의 다른 관리자를 선택하세요.");
        var r=new RequestAmendment();r.setEmployee(a);r.setApprover(approver);r.setRequestType(type);r.setRequestId(id);r.setOriginalVersion(o.version());r.setOriginalSummary(o.summary());r.setAction(action);r.setReason(text(reason,1000));r.setCreatedAt(LocalDateTime.now(clock));
        if(action.equals("CHANGE")){
            require(start!=null,"변경할 날짜를 입력하세요.");
            if(type.equals("LEAVE")){require(end!=null&&!end.isBefore(start)&&!start.isBefore(portal.today()),"오늘 이후의 올바른 휴가 기간을 선택하세요.");require(Arrays.stream(LeaveRequest.Kind.values()).anyMatch(k->k.name().equals(kind)),"휴가 종류를 선택하세요.");}
            else {require(startTime!=null&&endTime!=null&&endTime.isAfter(startTime)&&!start.atTime(endTime).isAfter(LocalDateTime.now(clock)),"이미 종료된 올바른 시간을 입력하세요.");if(type.equals("CORRECTION"))require(start.equals(o.startDate()),"출퇴근 정정은 같은 근무일의 시간만 변경할 수 있습니다.");}
            r.setStartDate(start);r.setEndDate(type.equals("LEAVE")?end:start);r.setLeaveKind(kind);r.setStartTime(startTime);r.setEndTime(endTime);
        }
        if(type.equals("CORRECTION")){var c=corrections.findById(id).orElseThrow();var record=attendance.findByEmployeeIdAndWorkDate(a.getId(),c.getWorkDate()).orElseThrow(()->new BusinessException("현재 출퇴근 기록이 없습니다."));require(Objects.equals(record.getCheckIn(),c.getProposedCheckIn())&&Objects.equals(record.getCheckOut(),c.getProposedCheckOut()),"승인 이후 출퇴근 기록이 변경되어 취소할 수 없습니다. 새 정정을 신청하세요.");r.setSourceAttendanceVersion(record.getVersion());}
        amendments.saveAndFlush(r);groupware.notify(approver,"승인된 신청의 취소·변경 요청: "+r.getTypeLabel(),"/groupware/amendments/"+r.getId());return r.getId();
    }
    @Transactional public void cancel(String login,Long id){
        var a=portal.current(login);var r=amendments.lockById(id).orElseThrow(()->new BusinessException("요청이 없습니다."));require(r.getEmployee().getId().equals(a.getId())&&r.getStatus().equals("PENDING"),"본인의 승인 대기 요청만 취소할 수 있습니다.");r.setStatus("CANCELLED");
    }
    @Transactional public void review(String login,Long id,boolean approve,String comment){
        var manager=departments.manager(login);var preliminary=amendments.findById(id).orElseThrow(()->new BusinessException("요청이 없습니다."));
        employees.lockById(preliminary.getEmployee().getId()).orElseThrow();var r=amendments.lockById(id).orElseThrow();departments.requireReview(manager,r.getEmployee(),r.getApprover());
        require(r.getStatus().equals("PENDING"),"이미 처리된 요청입니다.");comment=approve?(comment==null?"":comment.trim()):text(comment,500);require(comment.length()<=500,"의견은 500자 이내로 입력하세요.");
        if(approve){
            var o=original(r.getRequestType(),r.getRequestId());require(o.version()==r.getOriginalVersion(),"원본 신청이 변경되었습니다. 반려 후 다시 요청하세요.");
            require(o.employee().isActive()&&!o.employee().isDeleted(),"비활성 직원의 요청은 승인할 수 없습니다.");
            undo(r);
            if(r.getAction().equals("CHANGE"))replace(r,manager,comment);
        }
        r.setStatus(approve?"APPROVED":"REJECTED");r.setReviewer(manager);r.setReviewedAt(LocalDateTime.now(clock));r.setReviewComment(comment);
        groupware.notify(r.getEmployee(),"취소·변경 요청이 "+r.getStatusLabel()+"되었습니다: "+r.getTypeLabel(),"/groupware/amendments/"+id);
    }
    private void undo(RequestAmendment r){
        switch(r.getRequestType()){
            case "LEAVE"->{var old=leaves.lockById(r.getRequestId()).orElseThrow();old.setStatus(LeaveRequest.Status.CANCELLED);leaves.flush();}
            case "OVERTIME"->{var old=overtimes.lockById(r.getRequestId()).orElseThrow();old.setStatus(OvertimeRequest.Status.CANCELLED);overtimes.flush();}
            case "CORRECTION"->{
                var old=corrections.lockById(r.getRequestId()).orElseThrow();var a=attendance.findByEmployeeIdAndWorkDate(r.getEmployee().getId(),old.getWorkDate()).orElseThrow(()->new BusinessException("출퇴근 기록이 없습니다."));
                require(Objects.equals(r.getSourceAttendanceVersion(),a.getVersion())&&Objects.equals(a.getCheckIn(),old.getProposedCheckIn())&&Objects.equals(a.getCheckOut(),old.getProposedCheckOut()),"요청 이후 출퇴근 기록이 변경되었습니다. 반려 후 새 정정을 신청하세요.");
                if(old.getOriginalVersion()==null)attendance.delete(a);
                else {a.setCheckIn(old.getOriginalCheckIn());a.setCheckOut(old.getOriginalCheckOut());a.setLateArrival(a.getCheckIn().toLocalTime().isAfter(LocalTime.of(9,0)));a.setEarlyDeparture(a.getCheckOut()!=null&&a.getCheckOut().toLocalTime().isBefore(LocalTime.of(18,0)));}
                attendance.flush();old.setStatus(AttendanceCorrection.Status.CANCELLED);corrections.flush();
            }
            default->throw new BusinessException("신청 종류가 올바르지 않습니다.");
        }
    }
    private void replace(RequestAmendment r,Employee manager,String comment){
        String login=r.getEmployee().getLoginId();
        switch(r.getRequestType()){
            case "LEAVE"->{
                portal.requestLeave(login,manager.getId(),LeaveRequest.Kind.valueOf(r.getLeaveKind()),r.getStartDate(),r.getEndDate(),r.getReason());
                var next=leaves.findByEmployeeIdOrderByRequestedAtDesc(r.getEmployee().getId()).stream().max(Comparator.comparing(LeaveRequest::getId)).orElseThrow();portal.review(manager.getLoginId(),next.getId(),true,comment);r.setReplacementId(next.getId());
            }
            case "OVERTIME"->{
                overtime.request(login,manager.getId(),r.getStartDate(),r.getStartTime(),r.getEndTime(),r.getReason());
                var next=overtimes.findByEmployeeIdOrderByRequestedAtDesc(r.getEmployee().getId()).stream().max(Comparator.comparing(OvertimeRequest::getId)).orElseThrow();overtime.review(manager.getLoginId(),next.getId(),true,comment);r.setReplacementId(next.getId());
            }
            case "CORRECTION"->{
                correctionService.request(login,manager.getId(),r.getStartDate(),r.getStartTime(),r.getEndTime(),r.getReason());
                var next=corrections.findByEmployeeIdOrderByRequestedAtDesc(r.getEmployee().getId()).stream().max(Comparator.comparing(AttendanceCorrection::getId)).orElseThrow();correctionService.review(manager.getLoginId(),next.getId(),true,comment);r.setReplacementId(next.getId());
            }
        }
    }
}
