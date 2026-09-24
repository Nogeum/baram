package org.example.portal.repository;
import org.example.portal.domain.SharedDocument;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface SharedDocumentRepository extends JpaRepository<SharedDocument,Long> {
@Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from SharedDocument r where r.id=:id")
Optional<SharedDocument> lockById(@Param("id") Long id);
}

