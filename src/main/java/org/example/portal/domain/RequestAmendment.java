package org.example.portal.domain;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;
@Entity @Getter @Setter @Table(name="portal_amendment")
public class RequestAmendment extends BaseEntity {
@ManyToOne(optional=false) private Employee employee;
@ManyToOne(optional=false) private Employee approver;
@ManyToOne private Employee reviewer;
@Column(nullable=false,length=20) private String requestType;
@Column(nullable=false) private Long requestId;
@Column(nullable=false) private long originalVersion;
private Long sourceAttendanceVersion;
private Long replacementId;
@Column(nullable=false,length=20) private String action;
@Column(nullable=false,length=20) private String status="PENDING";
@Column(nullable=false,length=1000) private String reason;
@Column(nullable=false,length=2000) private String originalSummary;
private LocalDate startDate;
private LocalDate endDate;
@Column(length=20) private String leaveKind;
private LocalTime startTime;
private LocalTime endTime;
@Column(length=500) private String reviewComment;
@Column(nullable=false) private LocalDateTime createdAt;
private LocalDateTime reviewedAt;
public String getTypeLabel(){return switch(requestType){case "LEAVE"->"휴가";case "OVERTIME"->"초과근무";default->"출퇴근 정정";};}
public String getStatusLabel(){return switch(status){case "APPROVED"->"승인";case "REJECTED"->"반려";case "CANCELLED"->"취소";default->"승인 대기";};}
}
