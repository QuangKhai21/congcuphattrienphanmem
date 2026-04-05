package com.db7.j2ee_quanlythucung.repository;

import com.db7.j2ee_quanlythucung.entity.NutritionPlan;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NutritionPlanRepository extends JpaRepository<NutritionPlan, Long> {

    Page<NutritionPlan> findByPetId(Long petId, Pageable pageable);

    List<NutritionPlan> findByPetIdAndIsActiveTrue(Long petId);

    Optional<NutritionPlan> findByPetIdAndIsActiveTrueOrderByCreatedAtDesc(Long petId);
}
