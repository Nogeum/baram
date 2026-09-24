package org.example.portal.domain;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.*;
@Entity @Getter @Setter @Table(name="portal_file")
public class StoredFile extends BaseEntity {
@Column(nullable=false,length=20) private String ownerType;
@Column(nullable=false) private Long ownerId;
@ManyToOne(optional=false) private Employee uploader;
@Column(nullable=false,length=200) private String filename;
@Column(nullable=false) private long fileSize;
@Lob @Basic(fetch=FetchType.LAZY) @Column(nullable=false) private byte[] data;
@Column(nullable=false) private LocalDateTime createdAt;
}

