package com.db7.j2ee_quanlythucung.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "user_species", indexes = {
    @Index(name = "idx_species_user", columnList = "user_id"),
    @Index(name = "idx_species_name", columnList = "name")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSpecies {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 20)
    private String icon;  // e.g. "bi bi-heart-fill"

    @Column(length = 255)
    private String imageUrl;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** User sở hữu loài này */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User owner;

    @OneToMany(mappedBy = "species", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<UserBreed> breeds = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}