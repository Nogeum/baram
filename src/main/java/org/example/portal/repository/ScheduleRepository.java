package org.example.portal.repository;
import org.example.portal.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
import java.time.*;
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
List<Schedule> findByEmployeeIdAndEventDateBetweenOrderByEventDateAscStartTimeAsc(Long employeeId, LocalDate start, LocalDate end);
List<Schedule> findByEventDateBetweenOrderByEventDateAscStartTimeAsc(LocalDate start, LocalDate end);
List<Schedule> findByEmployeeDepartmentAndEmployeeRoleAndAssignedTrueAndEventDateBetweenOrderByEventDateAscStartTimeAsc(String department,Employee.Role role,LocalDate start,LocalDate end);
}
