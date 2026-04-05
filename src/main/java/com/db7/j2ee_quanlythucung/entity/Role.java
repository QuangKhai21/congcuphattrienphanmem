package com.db7.j2ee_quanlythucung.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private RoleType name;

    @Column(length = 200)
    private String description;

    public enum RoleType {
        ROLE_ADMIN,      // Quản trị hệ thống
        ROLE_MANAGER,    // Quản lý cửa hàng
        ROLE_STAFF,      // Nhân viên
        ROLE_VET,        // Bác sĩ thú y
        ROLE_CUSTOMER    // Khách hàng
    }
}
