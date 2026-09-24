package org.example.portal.repository;
import org.example.portal.domain.ResourceReservation;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface ResourceReservationRepository extends JpaRepository<ResourceReservation,Long> {
@Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from ResourceReservation r where r.id=:id")
Optional<ResourceReservation> lockById(@Param("id") Long id);
}

