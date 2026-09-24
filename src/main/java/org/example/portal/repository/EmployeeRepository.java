package org.example.portal.repository;
import org.example.portal.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
import java.time.*;
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
Optional<Employee> findByLoginId(String loginId);
List<Employee> findAllByOrderByNameAsc();
List<Employee> findByDeletedAtIsNullOrderByNameAsc();
List<Employee> findByDepartmentAndRoleOrderByNameAsc(String department,Employee.Role role);
List<Employee> findByRoleAndActiveTrue(Employee.Role role);
@Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select e from Employee e where e.id = :id")
Optional<Employee> lockById(@Param("id") Long id);
@Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select e from Employee e where e.role = :role order by e.id")
List<Employee> lockByRole(@Param("role") Employee.Role role);
}
