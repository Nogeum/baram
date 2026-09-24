package org.example.portal.domain;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;
@Entity @Getter @Setter @Table(name="portal_library")
public class SharedDocument extends BaseEntity {
@ManyToOne(optional=false) private Employee author;
@Column(nullable=false,length=160) private String title;
@Lob @Column(nullable=false) private String content;
@Column(nullable=false,length=20) private String visibility;
@Column(nullable=false,length=80) private String department;
@Column(nullable=false) private LocalDateTime createdAt;
@Column(nullable=false) private LocalDateTime updatedAt;
}

