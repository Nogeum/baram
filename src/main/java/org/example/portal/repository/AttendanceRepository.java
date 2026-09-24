package org.example.portal.repository;
import org.example.portal.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
import java.time.*;
public interface AttendanceRepository extends JpaRepository<Attendance, Long> {
List<Attendance> findByEmployeeDepartmentAndEmployeeRoleAndWorkDateBetweenOrderByWorkDateAsc(String department,Employee.Role role,LocalDate start,LocalDate end);
Optional<Attendance> findByEmployeeIdAndWorkDate(Long employeeId, LocalDate workDate);
List<Attendance> findTop7ByEmployeeIdOrderByWorkDateDesc(Long employeeId);
List<Attendance> findByEmployeeIdAndWorkDateBetweenOrderByWorkDateAsc(Long employeeId, LocalDate start, LocalDate end);
List<Attendance> findByWorkDateOrderByEmployeeNameAsc(LocalDate workDate);
List<Attendance> findByEmployeeDepartmentAndEmployeeRoleAndWorkDateOrderByEmployeeNameAsc(String department,Employee.Role role,LocalDate workDate);
}
