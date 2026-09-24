package org.example.portal.domain;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;
@Entity @Getter @Setter @Table(name="portal_resource")
public class BookableResource extends BaseEntity {
@Column(nullable=false,length=120) private String name;
@Column(nullable=false,length=20) private String kind;
@Column(length=200) private String location;
@Column(nullable=false) private int capacity=1;
@Column(nullable=false) private boolean active=true;
}

