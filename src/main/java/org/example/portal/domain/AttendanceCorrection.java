package org.example.portal.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;

@Entity @Getter @Setter @Table(name="portal_correction")
public class AttendanceCorrection extends BaseEntity {
    @ManyToOne(optional=false) private Employee employee;
    @ManyToOne(optional=false) private Employee approver;
    @ManyToOne private Employee reviewer;
    @Column(nullable=false) private LocalDate workDate;
    private Long originalVersion;
    private LocalDateTime originalCheckIn;
    private LocalDateTime originalCheckOut;
    @Column(nullable=false) private LocalDateTime proposedCheckIn;
    @Column(nullable=false) private LocalDateTime proposedCheckOut;
    @Column(nullable=false,length=1000) private String reason;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Status status=Status.PENDING;
    @Column(nullable=false) private LocalDateTime requestedAt;
    private LocalDateTime reviewedAt;
    @Column(length=500) private String reviewComment;
    public enum Status {
        PENDING("승인 대기"), APPROVED("승인"), REJECTED("반려"), CANCELLED("취소");
        private final String label;
        Status(String label){this.label=label;}
        public String getLabel(){return label;}
    }
}
