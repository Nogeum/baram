package org.example.portal.domain;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;
@Entity @Getter @Setter @Table(name="portal_document_step")
public class ApprovalStep extends BaseEntity {
@ManyToOne(optional=false) private ApprovalDocument document;
@ManyToOne(optional=false) private Employee reviewer;
@Column(nullable=false) private int stepOrder;
@Column(nullable=false,length=20) private String status="PENDING";
@Column(name="review_comment",length=500) private String comment;
private LocalDateTime reviewedAt;
}
