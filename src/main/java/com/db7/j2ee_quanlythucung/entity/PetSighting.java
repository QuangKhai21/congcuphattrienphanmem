package com.db7.j2ee_quanlythucung.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "pet_sightings", indexes = {
    @Index(name = "idx_sighting_report", columnList = "report_id"),
    @Index(name = "idx_sighting_location", columnList = "lat, lng")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PetSighting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private LostPetReport report;

    @Column(nullable = false)
    private LocalDateTime sightingTime;

    @Column(length = 500)
    private String location;

    @Column
    private Double lat;

    @Column
    private Double lng;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String contactInfo;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reported_by")
    private User reportedBy;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
