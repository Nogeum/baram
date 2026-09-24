package org.example.portal.repository;
import org.example.portal.domain.ApprovalDocument;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface ApprovalDocumentRepository extends JpaRepository<ApprovalDocument,Long> {
@Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from ApprovalDocument r where r.id=:id")
Optional<ApprovalDocument> lockById(@Param("id") Long id);
}

