package com.db7.j2ee_quanlythucung.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversation_messages", indexes = {
    @Index(name = "idx_msg_conv", columnList = "conversation_id"),
    @Index(name = "idx_msg_sent", columnList = "sentAt"),
    @Index(name = "idx_msg_sender", columnList = "sender_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConversationMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sender_id", nullable = false)
    private User sender;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SenderType senderType = SenderType.USER;

    public enum SenderType {
        USER,
        VET,
        AI
    }

    @Column(nullable = false, updatable = false)
    private LocalDateTime sentAt;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    @Column(columnDefinition = "TEXT")
    private String aiResponse;

    @PrePersist
    protected void onCreate() {
        sentAt = LocalDateTime.now();
    }
}
