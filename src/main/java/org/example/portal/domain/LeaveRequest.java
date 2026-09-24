package org.example.portal.domain;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;
@Entity @Getter @Setter
@Table(name="portal_leave")
public class LeaveRequest extends BaseEntity {
    @ManyToOne(optional=false) private Employee employee;
    @Column(nullable=false) private LocalDate startDate;
    @Column(nullable=false) private LocalDate endDate;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private Kind kind;
    @Column(nullable=false) private int chargedUnits;
    @Column(nullable=false, length=1000) private String reason;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private Status status = Status.PENDING;
    @Column(nullable=false) private LocalDateTime requestedAt;
    @ManyToOne private Employee reviewer;
    @ManyToOne private Employee approver;
    private LocalDateTime reviewedAt;
    @Column(length=500) private String reviewComment;
    public double getDays() { return chargedUnits / 2.0; }
    public enum Kind {
        ANNUAL("연차"), HALF_AM("오전 반차"), HALF_PM("오후 반차"), SICK("병가");
        private final String label;
        Kind(String label) { this.label=label; }
        public String getLabel() { return label; }
    }
    public enum Status {
        PENDING("승인 대기"), APPROVED("승인"), REJECTED("반려"), CANCELLED("취소");
        private final String label;
        Status(String label) { this.label=label; }
        public String getLabel() { return label; }
    }
}
