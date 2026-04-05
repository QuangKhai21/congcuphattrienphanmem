package com.db7.j2ee_quanlythucung.repository;

import com.db7.j2ee_quanlythucung.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {
}
