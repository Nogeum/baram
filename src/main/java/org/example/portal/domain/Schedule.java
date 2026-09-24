package org.example.portal.domain;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;
@Entity @Getter @Setter
@Table(name="portal_schedule")
public class Schedule extends BaseEntity {
    @ManyToOne(optional=false) private Employee employee;
    @Column(nullable=false, length=120) private String title;
    @Column(nullable=false) private LocalDate eventDate;
    @Column(length=8) private LocalTime startTime;
    @Column(length=8) private LocalTime endTime;
    @Column(nullable=false) private boolean assigned;
    @Column(length=500) private String memo;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=12)
    @org.hibernate.annotations.ColumnDefault("'BLUE'")
    private Color color = Color.BLUE;

    @Getter
    public enum Color {
        BLUE("#334eac", "파랑"), RED("#d9534f", "빨강"), GREEN("#4caf50", "초록"),
        ORANGE("#e08e0b", "주황"), PURPLE("#8e44ad", "보라"), TEAL("#16a085", "청록");
        private final String hex;
        private final String label;
        Color(String hex, String label) { this.hex=hex; this.label=label; }
    }
}
