package org.example.portal.domain;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;
@Entity @Getter @Setter @Table(name="portal_reservation")
public class ResourceReservation extends BaseEntity {
@ManyToOne(optional=false) private BookableResource resource;
@ManyToOne(optional=false) private Employee employee;
@Column(nullable=false,length=160) private String title;
@Column(nullable=false) private LocalDateTime startsAt;
@Column(nullable=false) private LocalDateTime endsAt;
@Column(nullable=false,length=20) private String status="PENDING";
@Column(nullable=false) private LocalDateTime createdAt;
@ManyToOne private Employee reviewer;
private LocalDateTime reviewedAt;
@Column(length=500) private String reviewComment;
public String getStatusLabel(){return switch(status){case "PENDING"->"승인 대기";case "ACTIVE"->"예약 확정";case "REJECTED"->"반려";default->"취소";};}
}

