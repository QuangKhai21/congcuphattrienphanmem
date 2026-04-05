package com.db7.j2ee_quanlythucung.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "conversations", indexes = {
    @Index(name = "idx_conv_created", columnList = "createdAt"),
    @Index(name = "idx_conv_type", columnList = "type"),
    @Index(name = "idx_conv_target_user", columnList = "target_user_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50, nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Type type = Type.VET;

    public enum Type {
        AI,       // Nekomimi AI chat
        VET,      // Chat với bác sĩ
        PRIVATE   // Chat riêng giữa 2 tài khoản
    }

    /**
     * Với PRIVATE: lưu ID của người tham gia còn lại (người gửi request là người đang đăng nhập,
     * participant kia lấy từ đây). Với VET thì dùng trường vet. Với AI thì trường này null.
     */
    @Column(name = "target_user_id")
    private Long targetUserId;

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ConversationParticipant> participants = new HashSet<>();

    @OneToMany(mappedBy = "conversation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private Set<ConversationMessage> messages = new HashSet<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_vet_id")
    private User vet;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime lastMessageAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastMessageAt = createdAt;
    }

    public void addParticipant(User user) {
        ConversationParticipant p = ConversationParticipant.builder()
                .conversation(this).user(user).build();
        this.participants.add(p);
    }

    public void addMessage(ConversationMessage msg) {
        this.messages.add(msg);
        this.lastMessageAt = msg.getSentAt();
    }
}
