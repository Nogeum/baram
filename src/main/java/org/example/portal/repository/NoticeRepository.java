package org.example.portal.repository;
import org.example.portal.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
import java.time.*;
public interface NoticeRepository extends JpaRepository<Notice, Long> {
List<Notice> findTop20ByOrderByCreatedAtDesc();
}

