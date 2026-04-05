package com.db7.j2ee_quanlythucung.repository;

import com.db7.j2ee_quanlythucung.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {

    boolean existsBySku(String sku);

    @Query("""
            SELECT p FROM Product p
            WHERE p.status = :status
              AND (:categoryId IS NULL OR p.category.id = :categoryId)
              AND (
                    :keyword IS NULL OR :keyword = ''
                    OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
                    OR LOWER(p.sku) LIKE LOWER(CONCAT('%', :keyword, '%'))
                  )
            """)
    Page<Product> searchActive(
            @Param("status") Product.ProductStatus status,
            @Param("categoryId") Long categoryId,
            @Param("keyword") String keyword,
            Pageable pageable);
}
