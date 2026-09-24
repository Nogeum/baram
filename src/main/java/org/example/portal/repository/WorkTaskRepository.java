package org.example.portal.repository;
import org.example.portal.domain.WorkTask;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface WorkTaskRepository extends JpaRepository<WorkTask,Long> {
@Query("select t from WorkTask t join fetch t.assignee e where e.department=:department and e.role=:role and e.deletedAt is null and t.dueDate between :start and :end order by t.dueDate, e.name, t.id")
List<WorkTask> departmentCalendar(@Param("department") String department,@Param("role") org.example.portal.domain.Employee.Role role,@Param("start") java.time.LocalDate start,@Param("end") java.time.LocalDate end);
@Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from WorkTask r where r.id=:id")
Optional<WorkTask> lockById(@Param("id") Long id);
}
