package com.db7.j2ee_quanlythucung.repository;

import com.db7.j2ee_quanlythucung.entity.PetCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PetCategoryRepository extends JpaRepository<PetCategory, Long> {
    Optional<PetCategory> findByCode(String code);
    boolean existsByCode(String code);
}
