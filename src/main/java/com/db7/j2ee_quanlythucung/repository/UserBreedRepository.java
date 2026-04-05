package com.db7.j2ee_quanlythucung.repository;

import com.db7.j2ee_quanlythucung.entity.UserBreed;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserBreedRepository extends JpaRepository<UserBreed, Long> {
    List<UserBreed> findBySpeciesIdOrderByNameAsc(Long speciesId);
    Optional<UserBreed> findByIdAndSpeciesOwnerId(Long id, Long ownerId);
    boolean existsByNameIgnoreCaseAndSpeciesId(String name, Long speciesId);
    void deleteByIdAndSpeciesOwnerId(Long id, Long ownerId);
    long countBySpeciesId(Long speciesId);
}