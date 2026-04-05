package com.db7.j2ee_quanlythucung.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "nutrition_plans", indexes = {
    @Index(name = "idx_nutrition_pet", columnList = "pet_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NutritionPlan {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @Column(length = 100)
    private String planName;

    @Column
    private LocalDate startDate;

    @Column
    private LocalDate endDate;

    @Column(columnDefinition = "TEXT")
    private String dailyMeals;

    @Column
    private Double dailyCalories;

    @Column(length = 20)
    private String calorieUnit;

    @Column(columnDefinition = "TEXT")
    private String foodRecommendations;

    @Column(columnDefinition = "TEXT")
    private String foodsToAvoid;

    @Column(columnDefinition = "TEXT")
    private String supplements;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @Column(nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
