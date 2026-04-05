package com.db7.j2ee_quanlythucung.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "products", indexes = {
        @Index(name = "idx_product_sku", columnList = "sku", unique = true),
        @Index(name = "idx_product_category", columnList = "category_id"),
        @Index(name = "idx_product_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, unique = true, length = 80)
    private String sku;

    @NotBlank
    @Column(nullable = false, length = 200)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ProductCategory category;

    @NotNull
    @DecimalMin("0.0")
    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal price;

    @DecimalMin("0.0")
    @Column(precision = 14, scale = 2)
    private BigDecimal salePrice;

    @Column
    @Builder.Default
    private Integer stockQuantity = 0;

    @Column(length = 30)
    private String unit;

    @Column(length = 20, nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private ProductStatus status = ProductStatus.ACTIVE;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String imageUrl;

    public enum ProductStatus {
        ACTIVE,
        INACTIVE,
        OUT_OF_STOCK
    }
}
