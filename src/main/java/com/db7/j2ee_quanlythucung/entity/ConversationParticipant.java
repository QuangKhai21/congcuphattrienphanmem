package com.db7.j2ee_quanlythucung.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "conversation_participants", indexes = {
    @Index(name = "idx_part_conv", columnList = "conversation_id"),
    @Index(name = "idx_part_user", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "joined_at", nullable = false, updatable = false)
    private java.time.LocalDateTime joinedAt;

    @Column
    @Builder.Default
    private Boolean isActive = true;

    @PrePersist
    protected void onCreate() {
        joinedAt = java.time.LocalDateTime.now();
    }
}
