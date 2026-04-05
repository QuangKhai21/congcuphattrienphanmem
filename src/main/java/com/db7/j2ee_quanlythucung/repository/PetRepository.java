package com.db7.j2ee_quanlythucung.repository;

import com.db7.j2ee_quanlythucung.entity.Pet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PetRepository extends JpaRepository<Pet, Long>, JpaSpecificationExecutor<Pet> {
    List<Pet> findByOwnerId(Long ownerId);
    Page<Pet> findByOwnerId(Long ownerId, Pageable pageable);
    Page<Pet> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

    @Query("SELECT p FROM Pet p WHERE p.owner.id = :ownerId AND (:status IS NULL OR p.status = :status)")
    Page<Pet> findByOwnerIdAndStatus(@Param("ownerId") Long ownerId, @Param("status") Pet.PetStatus status, Pageable pageable);

    @Query("SELECT COUNT(p) FROM Pet p")
    long countAllPets();

    @Query("SELECT COUNT(p) FROM Pet p WHERE p.status = :status")
    long countByStatus(@Param("status") Pet.PetStatus status);

    @Query("SELECT COUNT(p) FROM Pet p WHERE p.owner.id = :ownerId")
    long countByOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT p FROM Pet p WHERE p.owner.id = :ownerId ORDER BY p.createdAt DESC")
    List<Pet> findByOwnerIdOrderByCreatedAtDesc(@Param("ownerId") Long ownerId);

    @Query("SELECT p FROM Pet p ORDER BY p.createdAt DESC")
    List<Pet> findTop5ByOrderByCreatedAtDesc();

    @Query("SELECT COUNT(p) FROM Pet p WHERE p.category.id = :categoryId")
    long countByCategoryId(@Param("categoryId") Long categoryId);
}
