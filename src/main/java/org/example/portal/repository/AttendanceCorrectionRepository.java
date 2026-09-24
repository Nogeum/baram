package org.example.portal.repository;

import org.example.portal.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.time.LocalDate;
import java.util.*;

public interface AttendanceCorrectionRepository extends JpaRepository<AttendanceCorrection,Long> {
    List<AttendanceCorrection> findByEmployeeIdOrderByRequestedAtDesc(Long employeeId);
    boolean existsByEmployeeIdAndWorkDateAndStatus(Long employeeId,LocalDate date,AttendanceCorrection.Status status);
    List<AttendanceCorrection> findByEmployeeDepartmentAndStatusOrderByRequestedAtAsc(String department,AttendanceCorrection.Status status);
    @Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from AttendanceCorrection r where r.id=:id")
    Optional<AttendanceCorrection> lockById(@Param("id") Long id);
    @Modifying @Query("update AttendanceCorrection r set r.status=:cancelled, r.reviewComment='계정 삭제로 신청 취소', r.version=r.version+1 where r.employee.id=:id and r.status=:pending")
    int cancelForDeletedEmployee(@Param("id") Long id,@Param("pending") AttendanceCorrection.Status pending,@Param("cancelled") AttendanceCorrection.Status cancelled);
}
