package org.example.portal.domain;
import jakarta.persistence.*;
import lombok.Getter;
@MappedSuperclass @Getter
public abstract class BaseEntity {
    @Id @GeneratedValue(strategy=GenerationType.SEQUENCE, generator="portal_seq")
    @SequenceGenerator(name="portal_seq", sequenceName="portal_seq", allocationSize=1)
    private Long id;
    @Version private long version;
}

