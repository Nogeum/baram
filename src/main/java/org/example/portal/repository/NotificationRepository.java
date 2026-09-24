package org.example.portal.repository;
import org.example.portal.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import jakarta.persistence.LockModeType;
import java.util.*;
import java.time.*;
public interface NotificationRepository extends JpaRepository<Notification, Long> {
List<Notification> findTop100ByRecipientIdOrderByCreatedAtDesc(Long recipientId);
long countByRecipientIdAndReadFlagFalse(Long recipientId);
}
