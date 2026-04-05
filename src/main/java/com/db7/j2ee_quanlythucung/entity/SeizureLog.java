package com.db7.j2ee_quanlythucung.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "seizure_logs", indexes = {
    @Index(name = "idx_seizure_pet", columnList = "pet_id"),
    @Index(name = "idx_seizure_date", columnList = "seizureDate")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeizureLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @Column(nullable = false)
    private LocalDateTime seizureDate;

    @Column
    private Double durationMinutes;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private SeizureType type = SeizureType.GENERALIZED;

    @Column
    @Builder.Default
    private Integer severityScore = 5;

    @Column(columnDefinition = "TEXT")
    private String symptoms;

    @Column(columnDefinition = "TEXT")
    private String triggers;

    @Column(columnDefinition = "TEXT")
    private String treatmentGiven;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum SeizureType {
        GENERALIZED,     // Động kinh toàn thể
        FOCAL,           // Động kinh cục bộ
        CLUSTER,         // Nhiều cơn liên tiếp
        STATUS           // Động kinh kéo dài (cấp cứu)
    }
}
