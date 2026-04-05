package com.db7.j2ee_quanlythucung.repository;

import com.db7.j2ee_quanlythucung.entity.ConversationMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, Long> {

    @Query("SELECT m FROM ConversationMessage m " +
           "WHERE m.conversation.id = :conversationId " +
           "ORDER BY m.sentAt ASC")
    Page<ConversationMessage> findByConversationId(@Param("conversationId") Long conversationId, Pageable pageable);

    // Alias với tên khác để tránh conflict
    @Query("SELECT m FROM ConversationMessage m " +
           "WHERE m.conversation.id = :conversationId " +
           "ORDER BY m.sentAt ASC")
    Page<ConversationMessage> findByConversationIdOrderBySentAtAsc(@Param("conversationId") Long conversationId, Pageable pageable);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE ConversationMessage m SET m.isRead = true " +
           "WHERE m.conversation.id = :conversationId AND m.sender.id != :userId AND m.isRead = false")
    int markRead(@Param("conversationId") Long conversationId, @Param("userId") Long userId);

    @Query("SELECT COUNT(m) FROM ConversationMessage m " +
           "WHERE m.conversation.id = :conversationId AND m.isRead = false AND m.sender.id != :userId")
    long countUnread(@Param("conversationId") Long conversationId, @Param("userId") Long userId);
}
