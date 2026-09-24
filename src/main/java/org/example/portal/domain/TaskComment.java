package org.example.portal.domain;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;
@Entity @Getter @Setter @Table(name="portal_task_comment")
public class TaskComment extends BaseEntity {
@ManyToOne(optional=false) private WorkTask task;
@ManyToOne(optional=false) private Employee author;
@Lob @Column(nullable=false) private String content;
@Column(nullable=false) private LocalDateTime createdAt;
}

