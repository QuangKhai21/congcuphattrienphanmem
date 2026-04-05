package com.db7.j2ee_quanlythucung.repository;

import com.db7.j2ee_quanlythucung.entity.Conversation;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("SELECT DISTINCT c FROM Conversation c " +
           "JOIN c.participants p " +
           "WHERE p.user.id = :userId AND p.isActive = true " +
           "ORDER BY c.lastMessageAt DESC")
    Page<Conversation> findByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT DISTINCT c FROM Conversation c " +
           "JOIN c.participants p " +
           "WHERE p.user.id = :userId AND c.type = com.db7.j2ee_quanlythucung.entity.Conversation$Type.AI AND p.isActive = true " +
           "ORDER BY c.createdAt DESC")
    Page<Conversation> findAIByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT c FROM Conversation c " +
           "JOIN c.participants p " +
           "WHERE p.user.id = :userId AND c.type = :type AND p.isActive = true " +
           "ORDER BY c.lastMessageAt DESC")
    Page<Conversation> findByUserIdAndType(@Param("userId") Long userId,
                                            @Param("type") Conversation.Type type,
                                            Pageable pageable);

    @Query("SELECT c FROM Conversation c " +
           "JOIN c.participants p " +
           "WHERE p.user.id = :userId AND c.vet.id = :vetId AND c.type = 'VET' AND p.isActive = true " +
           "ORDER BY c.lastMessageAt DESC")
    Optional<Conversation> findByUserAndVet(@Param("userId") Long userId,
                                            @Param("vetId") Long vetId);

    @Query("SELECT c FROM Conversation c WHERE c.vet.id = :vetId AND c.type = 'VET'")
    Page<Conversation> findByVetId(@Param("vetId") Long vetId, Pageable pageable);

    @Query("SELECT COUNT(DISTINCT c.id) FROM Conversation c " +
           "JOIN c.participants p " +
           "WHERE c.vet.id = :vetId AND c.type = 'VET' AND p.isActive = true")
    long countConsultationsByVet(@Param("vetId") Long vetId);

    /**
     * Tìm cuộc trò chuyện PRIVATE giữa 2 user (không phân biết ai là người request).
     * Kết quả JOIN participants để đảm bảo cả 2 đều tham gia.
     */
    @Query("SELECT c FROM Conversation c " +
           "JOIN c.participants p1 ON p1.conversation = c AND p1.user.id = :userId1 AND p1.isActive = true " +
           "JOIN c.participants p2 ON p2.conversation = c AND p2.user.id = :userId2 AND p2.isActive = true " +
           "WHERE c.type = 'PRIVATE'")
    Optional<Conversation> findPrivateConversation(@Param("userId1") Long userId1,
                                                  @Param("userId2") Long userId2);

    /**
     * Tìm cuộc trò chuyện PRIVATE mà user hiện tại có targetUserId = :targetId (chiều ngược).
     */
    @Query("SELECT c FROM Conversation c " +
           "JOIN c.participants p ON p.conversation = c AND p.user.id = :userId AND p.isActive = true " +
           "WHERE c.type = 'PRIVATE' AND c.targetUserId = :targetId")
    Optional<Conversation> findPrivateConversationByTarget(@Param("userId") Long userId,
                                                         @Param("targetId") Long targetId);
}
