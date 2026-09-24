package org.example.portal.domain;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;
@Entity @Getter @Setter @Table(name="portal_document")
public class ApprovalDocument extends BaseEntity {
@ManyToOne(optional=false) private Employee author;
@Column(nullable=false,length=20) private String kind;
@Column(nullable=false,length=160) private String title;
@Lob @Column(nullable=false) private String content;
@Column(precision=15,scale=2) private java.math.BigDecimal amount;
private LocalDate startDate;
private LocalDate endDate;
@Column(nullable=false,length=20) private String status="PENDING";
@Column(nullable=false) private int currentStep=1;
@Column(nullable=false) private LocalDateTime createdAt;
public String getKindLabel(){return switch(kind){case "PURCHASE"->"구매 요청";case "EXPENSE"->"지출결의";default->"출장 신청";};}
public String getStatusLabel(){return switch(status){case "APPROVED"->"승인";case "REJECTED"->"반려";case "CANCELLED"->"취소";default->"결재 대기";};}
}

