package com.db7.j2ee_quanlythucung.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "vet_questions", indexes = {
    @Index(name = "idx_question_pet", columnList = "pet_id"),
    @Index(name = "idx_question_author", columnList = "author_id"),
    @Index(name = "idx_question_category", columnList = "category"),
    @Index(name = "idx_question_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VetQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 300)
    private String title;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id")
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_id", nullable = false)
    private User author;

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private QuestionCategory category = QuestionCategory.GENERAL;

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private QuestionStatus status = QuestionStatus.PENDING;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime answeredAt;

    @Column(nullable = false)
    @Builder.Default
    private Integer viewCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer likeCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "answered_by")
    private User answeredBy;

    @OneToOne(mappedBy = "question", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private VetAnswer answer;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isPublic = true;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum QuestionCategory {
        GENERAL,           // Hỏi đáp chung
        NUTRITION,         // Dinh dưỡng
        BEHAVIOR,          // Hành vi
        HEALTH,            // Sức khỏe
        DENTAL,            // Răng miệng
        SKIN,              // Da lông
        DIGESTION,         // Tiêu hóa
        VACCINATION,       // Tiêm phòng
        EMERGENCY,         // Cấp cứu
        ELDERLY_PET        // Thú cưng già
    }

    public enum QuestionStatus {
        PENDING,   // Chờ trả lời
        ANSWERED,  // Đã trả lời
        CLOSED,    // Đã đóng
        FLAGGED    // Bị báo cáo
    }
}
