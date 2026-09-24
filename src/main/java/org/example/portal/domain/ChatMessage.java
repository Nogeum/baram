package org.example.portal.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Entity @Getter @Setter
@Table(name="portal_chat_message", uniqueConstraints=@UniqueConstraint(name="uk_chat_sender_client",columnNames={"sender_id","client_id"}))
public class ChatMessage extends BaseEntity {
    @ManyToOne(optional=false) private Employee sender;
    @ManyToOne(optional=false) private Employee recipient;
    @Lob @Column(nullable=false) private String content;
    @Column(name="client_id",nullable=false,length=36) private String clientId;
    @Column(nullable=false) private LocalDateTime sentAt;
    private LocalDateTime readAt;
}
