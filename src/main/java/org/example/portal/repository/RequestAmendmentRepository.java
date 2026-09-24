package org.example.portal.repository;
import org.example.portal.domain.RequestAmendment;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface RequestAmendmentRepository extends JpaRepository<RequestAmendment,Long> {
@Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from RequestAmendment r where r.id=:id")
Optional<RequestAmendment> lockById(@Param("id") Long id);
}

