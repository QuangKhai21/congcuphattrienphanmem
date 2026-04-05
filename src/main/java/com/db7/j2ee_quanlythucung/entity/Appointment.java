package com.db7.j2ee_quanlythucung.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "appointments", indexes = {
    @Index(name = "idx_appointment_pet", columnList = "pet_id"),
    @Index(name = "idx_appointment_date", columnList = "appointment_date"),
    @Index(name = "idx_appointment_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    private User staff;

    @Column(nullable = false)
    private LocalDateTime appointmentDate;

    @Column(length = 50)
    @Enumerated(EnumType.STRING)
    private AppointmentType type;

    @Column(length = 500)
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(length = 20, nullable = false)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private AppointmentStatus status = AppointmentStatus.SCHEDULED;

    /** 用户已在导航栏通知中查看过（用于清除红点） */
    @Column(nullable = false)
    @Builder.Default
    private boolean headerNotificationSeen = false;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    public enum AppointmentType {
        CHECKUP,        // Khám tổng quát
        VACCINATION,    // Tiêm phòng
        GROOMING,       // Chăm sóc lông
        SURGERY,        // Phẫu thuật
        DENTAL,         // Chăm sóc răng
        OTHER
    }

    public enum AppointmentStatus {
        SCHEDULED,      // Đã đặt
        CONFIRMED,      // Đã xác nhận
        IN_PROGRESS,    // Đang thực hiện
        COMPLETED,      // Hoàn thành
        CANCELLED,      // Đã hủy
        NO_SHOW         // Không đến
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
