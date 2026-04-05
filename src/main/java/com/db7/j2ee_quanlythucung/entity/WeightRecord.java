package com.db7.j2ee_quanlythucung.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "weight_records", indexes = {
    @Index(name = "idx_weight_pet", columnList = "pet_id"),
    @Index(name = "idx_weight_date", columnList = "record_date")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WeightRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @NotNull(message = "Vui lòng chọn ngày ghi nhận")
    @Column(nullable = false)
    private LocalDate recordDate;

    @NotBlank(message = "Vui lòng nhập cân nặng")
    @Column(nullable = false, length = 10)
    private String weight;

    @Column(length = 10)
    private String unit;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by")
    private User recordedBy;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (unit == null) {
            unit = "kg";
        }
    }
}
