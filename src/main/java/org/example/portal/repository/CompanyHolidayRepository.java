package org.example.portal.repository;
import org.example.portal.domain.CompanyHoliday;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
public interface CompanyHolidayRepository extends JpaRepository<CompanyHoliday,Long> {
@Lock(LockModeType.PESSIMISTIC_WRITE) @Query("select r from CompanyHoliday r where r.id=:id")
Optional<CompanyHoliday> lockById(@Param("id") Long id);
}

