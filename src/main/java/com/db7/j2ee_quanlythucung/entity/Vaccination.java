package com.db7.j2ee_quanlythucung.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "vaccinations", indexes = {
    @Index(name = "idx_vaccination_pet", columnList = "pet_id"),
    @Index(name = "idx_vaccination_date", columnList = "vaccination_date"),
    @Index(name = "idx_vaccination_next", columnList = "next_due_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vaccination {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "administered_by")
    private User administeredBy;

    @Column(nullable = false, length = 100)
    private String vaccineName;

    @Enumerated(EnumType.STRING)
    @Column(length = 50)
    private VaccineType vaccineType;

    @Column(nullable = false)
    private LocalDate vaccinationDate;

    @Column
    private LocalDate nextDueDate;

    @Column(length = 50)
    private String batchNumber;

    @Column(length = 100)
    private String manufacturer;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column
    private Boolean reminderSent;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum VaccineType {
        RABIES,           // Dại
        DHPP,             // Phòng 6 bệnh cho chó
        FVRCP,            // Phòng 4 bệnh cho mèo
        BORDETELLA,       // Ho Kennel
        LEPTOSPIROSIS,    // Leptospira
        CANINE_INFLUENZA, // Cúm chó
        LYME,             // Bệnh Lyme
        FELINE_LEUKEMIA,  // Leukemia mèo
        OTHER
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (reminderSent == null) {
            reminderSent = false;
        }
    }
}
