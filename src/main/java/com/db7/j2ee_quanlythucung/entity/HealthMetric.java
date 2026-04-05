package com.db7.j2ee_quanlythucung.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "health_metrics", indexes = {
    @Index(name = "idx_metric_pet", columnList = "pet_id"),
    @Index(name = "idx_metric_type", columnList = "metricType"),
    @Index(name = "idx_metric_date", columnList = "recordedAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class HealthMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @Column(length = 30, nullable = false)
    @Enumerated(EnumType.STRING)
    private MetricType metricType;

    @Column(name = "metric_value", nullable = false)
    private Double value;

    @Column(length = 20)
    @Builder.Default
    private String unit = "";

    @Column(nullable = false)
    private LocalDateTime recordedAt;

    @Column
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by")
    private User recordedBy;

    @Column(columnDefinition = "TEXT")
    private String aiAnalysis;

    @Column
    @Builder.Default
    private Boolean alertTriggered = false;

    @Column
    private String alertMessage;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public enum MetricType {
        WEIGHT,           // Cân nặng (kg)
        HEIGHT,           // Chiều cao (cm)
        TEMPERATURE,      // Nhiệt độ (°C)
        HEART_RATE,       // Nhịp tim (bpm)
        RESPIRATION,      // Nhịp thở (lần/phút)
        APPETITE,         // Ăn uống (1-10)
        ACTIVITY,         // Hoạt động (1-10)
        SLEEP_QUALITY,    // Chất lượng giấc ngủ (1-10)
        WATER_INTAKE,     // Lượng nước uống (ml)
        BOWEL_MOVEMENT,   // Đi vệ sinh (1-5)
        SEIZURE_COUNT,    // Số cơn động kinh
        SEIZURE_DURATION, // Thời gian cơn (phút)
        SEIZURE_SEVERITY, // Mức độ cơn (1-10)
        MEDICATION_DOSE,   // Liều thuốc (mg)
        BLOOD_PRESSURE,   // Huyết áp (mmHg)
        BLOOD_SUGAR       // Đường huyết (mg/dL)
    }
}
