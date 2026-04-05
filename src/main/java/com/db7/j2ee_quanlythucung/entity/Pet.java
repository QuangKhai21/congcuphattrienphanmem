package com.db7.j2ee_quanlythucung.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pets", indexes = {
    @Index(name = "idx_pet_owner", columnList = "owner_id"),
    @Index(name = "idx_pet_category", columnList = "category_id"),
    @Index(name = "idx_pet_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private PetCategory category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "breed_id")
    private Breed breed;

    /** Loài tự tạo (thay thế category nếu user chọn loài tự tạo) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_species_id")
    private UserSpecies userSpecies;

    /** Giống tự tạo (thay thế breed nếu user chọn giống tự tạo) */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_breed_id")
    private UserBreed userBreed;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(length = 10)
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @Column
    private LocalDate dateOfBirth;

    @Column(length = 50)
    private String color;

    @Column(length = 10)
    private String weight;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(length = 500)
    private String imageUrl;

    @Column(length = 500)
    private String avatarUrl;

    @Column(length = 20, nullable = false)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private PetStatus status = PetStatus.ACTIVE;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "pet", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MedicalRecord> medicalRecords = new ArrayList<>();

    @OneToMany(mappedBy = "pet", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Appointment> appointments = new ArrayList<>();

    public enum Gender {
        MALE, FEMALE, UNKNOWN
    }

    public enum PetStatus {
        ACTIVE,     // Đang nuôi
        PASSED,     // Đã mất
        TRANSFERRED // Đã chuyển nhượng
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
