package org.example.portal.repository;
import org.example.portal.domain.OvertimeRequest;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.*;
public interface OvertimeRequestRepository extends JpaRepository<OvertimeRequest,Long> {
    @Modifying @Query("update OvertimeRequest r set r.status = :cancelled, r.reviewComment = '계정 삭제로 신청 취소', r.version = r.version + 1 where r.employee.id = :id and r.status = :pending")
    int cancelPendingForDeletedEmployee(@Param("id") Long id,@Param("pending") OvertimeRequest.Status pending,@Param("cancelled") OvertimeRequest.Status cancelled);
    List<OvertimeRequest> findByEmployeeDepartmentAndEmployeeRoleAndStatusOrderByRequestedAtAsc(String department,org.example.portal.domain.Employee.Role role,OvertimeRequest.Status status);
    List<OvertimeRequest> findByEmployeeIdOrderByRequestedAtDesc(Long employeeId);
    List<OvertimeRequest> findByApproverIdAndStatusOrderByRequestedAtAsc(Long approverId,OvertimeRequest.Status status);
    List<OvertimeRequest> findByEmployeeIdAndWorkDate(Long employeeId,LocalDate workDate);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from OvertimeRequest r where r.id=:id")
    Optional<OvertimeRequest> lockById(@Param("id") Long id);
}
