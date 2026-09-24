package org.example.portal.domain;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;
@Entity @Getter @Setter
@Table(name="portal_notice")
public class Notice extends BaseEntity {
    @Column(nullable=false, length=160) private String title;
    @Lob @Column(nullable=false) private String content;
    @Column(nullable=false) private LocalDateTime createdAt;
    @ManyToOne(optional=false) private Employee author;
}
