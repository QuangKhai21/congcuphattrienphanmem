package com.db7.j2ee_quanlythucung.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "pet_services", indexes = {
    @Index(name = "idx_service_type", columnList = "serviceType"),
    @Index(name = "idx_service_location", columnList = "lat, lng"),
    @Index(name = "idx_service_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PetService {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 20)
    @Enumerated(EnumType.STRING)
    private ServiceType serviceType;

    @Column(length = 500)
    private String address;

    @Column
    private Double lat;

    @Column
    private Double lng;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String hotline;

    @Column(length = 200)
    private String website;

    @Column(length = 100)
    private String email;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column
    private String imageUrl;

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ServiceStatus status = ServiceStatus.ACTIVE;

    @Column
    @Builder.Default
    private Boolean is24h = false;

    @Column
    @Builder.Default
    private Boolean hasPickup = false;

    @Column
    private Double rating;

    @Column
    private Integer reviewCount;

    @Column
    private String priceRange;

    @Column(length = 20)
    private String openTime;

    @Column(length = 20)
    private String closeTime;

    @Column
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    public enum ServiceType {
        PET_SHOP,       // Cửa hàng thú cưng
        SPA,            // Spa & Grooming
        HOTEL,          // Khách sạn thú cưng
        PET_TAXI,       // Pet Taxi
        VET_CLINIC,     // Phòng khám thú y
        TRAINING        // Trung tâm dạy thú cưng
    }

    public enum ServiceStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED
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
