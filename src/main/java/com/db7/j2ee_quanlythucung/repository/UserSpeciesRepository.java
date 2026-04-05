package com.db7.j2ee_quanlythucung.repository;

import com.db7.j2ee_quanlythucung.entity.UserSpecies;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSpeciesRepository extends JpaRepository<UserSpecies, Long> {
    List<UserSpecies> findByOwnerIdOrderByNameAsc(Long ownerId);
    Optional<UserSpecies> findByIdAndOwnerId(Long id, Long ownerId);
    boolean existsByNameIgnoreCaseAndOwnerId(String name, Long ownerId);
    void deleteByIdAndOwnerId(Long id, Long ownerId);
}