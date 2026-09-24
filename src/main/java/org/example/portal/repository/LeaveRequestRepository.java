package org.example.portal.repository;
import org.example.portal.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
import java.time.*;
public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
@Query("select r from LeaveRequest r where r.employee.department=:department and r.employee.role=:role and r.status=:status and r.startDate<=:end and r.endDate>=:start order by r.startDate, r.employee.name")
List<LeaveRequest> departmentApproved(@Param("department") String department,@Param("role") Employee.Role role,@Param("status") LeaveRequest.Status status,@Param("start") LocalDate start,@Param("end") LocalDate end);
@Modifying @Query("update LeaveRequest r set r.status = :cancelled, r.reviewComment = '계정 삭제로 신청 취소', r.version = r.version + 1 where r.employee.id = :id and r.status = :pending")
int cancelPendingForDeletedEmployee(@Param("id") Long id,@Param("pending") LeaveRequest.Status pending,@Param("cancelled") LeaveRequest.Status cancelled);
List<LeaveRequest> findByEmployeeIdOrderByRequestedAtDesc(Long employeeId);
List<LeaveRequest> findByStatusOrderByRequestedAtAsc(LeaveRequest.Status status);
List<LeaveRequest> findByEmployeeDepartmentAndEmployeeRoleAndStatusOrderByRequestedAtAsc(String department,Employee.Role role,LeaveRequest.Status status);
@Query("select r from LeaveRequest r left join r.approver a where r.status = :status and (a.id = :approverId or r.approver is null) order by r.requestedAt asc")
List<LeaveRequest> pendingForApprover(@Param("approverId") Long approverId,@Param("status") LeaveRequest.Status status);
@Query("select r from LeaveRequest r where r.employee.id = :employeeId and r.startDate >= :start and r.startDate <= :end")
List<LeaveRequest> inYear(@Param("employeeId") Long employeeId, @Param("start") LocalDate start, @Param("end") LocalDate end);
@Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from LeaveRequest r where r.id = :id")
Optional<LeaveRequest> lockById(@Param("id") Long id);
}
