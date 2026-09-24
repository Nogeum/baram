package org.example.portal.domain;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;
@Entity @Getter @Setter @Table(name="portal_holiday")
public class CompanyHoliday extends BaseEntity {
@Column(nullable=false,unique=true) private LocalDate holidayDate;
@Column(nullable=false,length=120) private String name;
@Column(nullable=false,length=20) private String kind;
@ManyToOne(optional=false) private Employee author;
@Column(nullable=false) private LocalDateTime createdAt;
}

