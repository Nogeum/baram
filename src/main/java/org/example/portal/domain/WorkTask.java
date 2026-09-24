package org.example.portal.domain;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;
@Entity @Getter @Setter @Table(name="portal_task")
public class WorkTask extends BaseEntity {
@ManyToOne(optional=false) private Employee creator;
@ManyToOne(optional=false) private Employee assignee;
@Column(nullable=false,length=160) private String title;
@Lob @Column(nullable=false) private String description;
@Column(nullable=false) private LocalDate dueDate;
@Column(nullable=false,length=20) private String status="WAITING";
@Column(nullable=false) private LocalDateTime createdAt;
private LocalDate reminderDate;
public String getStatusLabel(){return switch(status){case "IN_PROGRESS"->"진행 중";case "DONE"->"완료";default->"대기";};}
}

