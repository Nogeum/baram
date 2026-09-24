package org.example.portal.repository;
import org.example.portal.domain.ApprovalStep;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface ApprovalStepRepository extends JpaRepository<ApprovalStep,Long> {
@Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from ApprovalStep r where r.id=:id")
Optional<ApprovalStep> lockById(@Param("id") Long id);
}

