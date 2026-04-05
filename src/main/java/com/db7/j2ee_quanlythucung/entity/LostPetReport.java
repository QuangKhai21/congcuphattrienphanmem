package com.db7.j2ee_quanlythucung.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "lost_pet_reports", indexes = {
    @Index(name = "idx_lost_pet_status", columnList = "status"),
    @Index(name = "idx_lost_pet_location", columnList = "last_seen_lat, last_seen_lng"),
    @Index(name = "idx_lost_pet_pet", columnList = "pet_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LostPetReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @Column(nullable = false)
    private LocalDateTime lostDate;

    @Column(length = 500)
    private String lastSeenLocation;

    @Column
    private Double lastSeenLat;

    @Column
    private Double lastSeenLng;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String reward;

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ReportStatus status = ReportStatus.ACTIVE;

    @Column(nullable = false)
    @Builder.Default
    private Boolean contactable = true;

    @Column(length = 500)
    private String contactPhone;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime resolvedAt;

    @Column
    private String resolvedNotes;

    public enum ReportStatus {
        ACTIVE,        // Đang tìm kiếm
        FOUND,          // Đã tìm thấy
        CLOSED,         // Đã đóng (hết hạn hoặc chủ tự giải quyết)
        SIGHTING        // Chỉ là tin nhìn thấy
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
