package org.example.portal.repository;

import org.example.portal.domain.ChatMessage;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import java.time.LocalDateTime;
import java.util.*;

public interface ChatMessageRepository extends JpaRepository<ChatMessage,Long> {
    long countByRecipientIdAndReadAtIsNull(Long recipientId);
    Optional<ChatMessage> findBySenderIdAndClientId(Long senderId,String clientId);
    @Query("select m from ChatMessage m join fetch m.sender join fetch m.recipient where ((m.sender.id=:me and m.recipient.id=:peer) or (m.sender.id=:peer and m.recipient.id=:me)) and m.id<:before order by m.id desc")
    List<ChatMessage> history(@Param("me") Long me,@Param("peer") Long peer,@Param("before") Long before,Pageable page);
    @Query("select m.recipient.id,max(m.id) from ChatMessage m where m.sender.id=:me group by m.recipient.id")
    List<Object[]> sentThreads(@Param("me") Long me);
    @Query("select m.sender.id,max(m.id),sum(case when m.readAt is null then 1 else 0 end) from ChatMessage m where m.recipient.id=:me group by m.sender.id")
    List<Object[]> receivedThreads(@Param("me") Long me);
    @Query("select m from ChatMessage m join fetch m.sender join fetch m.recipient where m.id in :ids")
    List<ChatMessage> summaries(@Param("ids") Collection<Long> ids);
    @Modifying
    @Query("update ChatMessage m set m.readAt=:now,m.version=m.version+1 where m.recipient.id=:me and m.sender.id=:peer and m.readAt is null and m.id in :ids")
    int markRead(@Param("me") Long me,@Param("peer") Long peer,@Param("ids") Collection<Long> ids,@Param("now") LocalDateTime now);
}
