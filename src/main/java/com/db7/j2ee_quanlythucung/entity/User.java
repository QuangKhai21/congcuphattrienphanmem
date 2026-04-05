package com.db7.j2ee_quanlythucung.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users", indexes = {
    @Index(name = "idx_user_email", columnList = "email"),
    @Index(name = "idx_user_username", columnList = "username")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(unique = true, nullable = false, length = 100)
    private String username;

    @NotBlank
    @Column(nullable = false, length = 255)
    private String password;

    @NotBlank
    @Column(nullable = false, length = 100)
    private String fullName;

    @Email
    @Column(unique = true, nullable = false, length = 150)
    private String email;

    @Column(length = 20)
    private String phone;

    @Column(length = 500)
    private String address;

    @Column(length = 500)
    private String avatarUrl;

    @Column(nullable = false)
    @Builder.Default
    private Boolean enabled = true;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column
    private LocalDateTime updatedAt;

    @Column
    private LocalDateTime lastLoginAt;

    @Column(length = 20)
    private String provider;

    @Column(length = 100)
    private String providerId;

    /* ===== Trường dành cho Bác sĩ thú y ===== */
    @Column
    @Builder.Default
    private Boolean isVet = false;

    @Column(length = 100)
    private String specialization;

    @Column(length = 100)
    private String province;

    @Column(length = 100)
    private String city;

    @Column(columnDefinition = "TEXT")
    private String bio;

    @Column(length = 200)
    private String clinicName;

    @Column(length = 200)
    private String clinicAddress;

    @Column(length = 200)
    private String licenseNumber;

    @Column
    @Builder.Default
    private Integer yearsExperience = 0;

    @Column
    @Builder.Default
    private Double rating = 0.0;

    @Column
    @Builder.Default
    private Integer ratingCount = 0;

    /* ===== Trạng thái phê duyệt bác sĩ ===== */
    /**
     * Đã gửi yêu cầu đăng ký bác sĩ, đang chờ admin duyệt.
     */
    @Column
    @Builder.Default
    private Boolean isVetPending = false;

    /**
     * Admin chặn không cho tài khoản này đăng ký làm bác sĩ.
     */
    @Column
    @Builder.Default
    private Boolean vetRegistrationBlocked = false;

    /**
     * Ghi chú lý do từ chối đăng ký bác sĩ (do admin nhập).
     */
    @Column(length = 500)
    private String vetRequestNote;

    /**
     * Từng được duyệt / công nhận là bác sĩ (dùng để hiển thị dòng "đã chặn" trong trang quản trị).
     */
    @Column
    @Builder.Default
    private Boolean vetEverApproved = false;

    /**
     * Đã từng là bác sĩ và bị hủy bởi admin (dùng để hiển thị tab "Bị hủy" trong trang quản trị).
     */
    @Column
    @Builder.Default
    private Boolean vetRemoved = false;

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
