package org.example.portal.repository;
import org.example.portal.domain.TaskComment;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface TaskCommentRepository extends JpaRepository<TaskComment,Long> {
@Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from TaskComment r where r.id=:id")
Optional<TaskComment> lockById(@Param("id") Long id);
}

