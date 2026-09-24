package org.example.portal.domain;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;
@Entity @Getter @Setter
@Table(name="portal_employee", uniqueConstraints=@UniqueConstraint(name="uk_employee_login", columnNames="login_id"))
public class Employee extends BaseEntity {
    @Column(name="login_id", nullable=false, length=60) private String loginId;
    @Column(nullable=false, length=100) private String password;
    @Column(nullable=false, length=80) private String name;
    @Column(nullable=false, length=80) private String department;
    @Column(nullable=false, length=40) private String positionName;
    @Enumerated(EnumType.STRING) @Column(nullable=false, length=20) private Role role = Role.EMPLOYEE;
    @Column(nullable=false) private boolean active = true;
    @Column(nullable=false) @org.hibernate.annotations.ColumnDefault("0") private boolean departmentManager = false;
    @Column(length=120) private String email;
    @Column(length=30) private String phone;
    @Column(length=30) private String extensionNumber;
    @Column(length=500) private String jobDescription;
    private LocalDate hireDate;
    private Long profileFileId;
    public String getProfileImageUrl() { return profileFileId==null ? null : "/profiles/"+getId()+"/image?v="+profileFileId; }
    @Column(nullable=false) private int annualUnits = 30;
    private LocalDateTime deletedAt;
    @Column(length=60) private String deletedByLogin;
    public boolean isDivisionHead() { return role==Role.EMPLOYEE && departmentManager && "본부장".equals(positionName); }
    public boolean isDeleted() { return deletedAt!=null; }
    public String getDisplayName() { return name+(isDeleted() ? " (삭제된 계정)" : ""); }
    public enum Role { EMPLOYEE, ADMIN }
}
