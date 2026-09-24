package org.example.portal.domain;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;
@Entity @Getter @Setter
@Table(name="portal_attendance", uniqueConstraints=@UniqueConstraint(name="uk_attendance_day", columnNames={"employee_id","work_date"}))
public class Attendance extends BaseEntity {
    @ManyToOne(optional=false) @JoinColumn(name="employee_id") private Employee employee;
    @Column(name="work_date", nullable=false) private LocalDate workDate;
    @Column(nullable=false) private LocalDateTime checkIn;
    private LocalDateTime checkOut;
    @Column(nullable=false) private boolean lateArrival;
    @Column(nullable=false) private boolean earlyDeparture;
    public String getStatus() { return (lateArrival ? "지각" : "정상 출근") + (checkOut == null ? " · 근무 중" : earlyDeparture ? " · 조퇴" : " · 퇴근"); }
}

