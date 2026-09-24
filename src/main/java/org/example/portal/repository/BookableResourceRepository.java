package org.example.portal.repository;
import org.example.portal.domain.BookableResource;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface BookableResourceRepository extends JpaRepository<BookableResource,Long> {
@Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from BookableResource r where r.id=:id")
Optional<BookableResource> lockById(@Param("id") Long id);
}

