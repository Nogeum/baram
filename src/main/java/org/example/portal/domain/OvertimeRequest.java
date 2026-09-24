package org.example.portal.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;

@Entity @Getter @Setter @Table(name="portal_overtime")
public class OvertimeRequest extends BaseEntity {
    @ManyToOne(optional=false) private Employee employee;
    @ManyToOne(optional=false) private Employee approver;
    @Column(nullable=false) private LocalDate workDate;
    @Column(nullable=false) private LocalTime startTime;
    @Column(nullable=false) private LocalTime endTime;
    @Column(nullable=false,length=1000) private String reason;
    @Enumerated(EnumType.STRING) @Column(nullable=false,length=20) private Status status=Status.PENDING;
    @Column(nullable=false) private LocalDateTime requestedAt;
    private LocalDateTime reviewedAt;
    @Column(length=500) private String reviewComment;
    public long getMinutes() { return Duration.between(startTime,endTime).toMinutes(); }
    public String getDurationLabel() { return getMinutes()/60+"시간 "+getMinutes()%60+"분"; }
    public enum Status {
        PENDING("승인 대기"), APPROVED("승인"), REJECTED("반려"), CANCELLED("취소");
        private final String label;
        Status(String label) { this.label=label; }
        public String getLabel() { return label; }
    }
}
