package org.example.portal.domain;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;
@Entity @Getter @Setter
@Table(name="portal_notification")
public class Notification extends BaseEntity {
    @ManyToOne(optional=false) private Employee recipient;
    @Column(nullable=false, length=300) private String message;
    @Column(nullable=false) private LocalDateTime createdAt;
    @Column(nullable=false) private boolean readFlag;
    @Column(length=200) private String targetPath;
}
