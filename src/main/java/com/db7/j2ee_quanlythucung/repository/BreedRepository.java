package com.db7.j2ee_quanlythucung.repository;

import com.db7.j2ee_quanlythucung.entity.Breed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface BreedRepository extends JpaRepository<Breed, Long> {
    List<Breed> findByCategoryId(Long categoryId);
    boolean existsByNameIgnoreCaseAndCategoryId(String name, Long categoryId);
}
