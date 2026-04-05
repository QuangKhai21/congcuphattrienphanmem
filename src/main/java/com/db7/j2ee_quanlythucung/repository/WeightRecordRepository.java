package com.db7.j2ee_quanlythucung.repository;

import com.db7.j2ee_quanlythucung.entity.WeightRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeightRecordRepository extends JpaRepository<WeightRecord, Long> {

    List<WeightRecord> findByPetIdOrderByRecordDateAsc(Long petId);

    List<WeightRecord> findByPetIdAndRecordDateGreaterThanEqualOrderByRecordDateAsc(Long petId, LocalDate since);

    @Query("SELECT w FROM WeightRecord w WHERE w.pet.owner.id = :ownerId ORDER BY w.recordDate DESC")
    List<WeightRecord> findByOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT w FROM WeightRecord w WHERE w.pet.id = :petId ORDER BY w.recordDate DESC")
    List<WeightRecord> findByPetId(@Param("petId") Long petId);

    Optional<WeightRecord> findTop1ByPetIdOrderByRecordDateDesc(Long petId);

    @Query("SELECT COUNT(w) FROM WeightRecord w WHERE w.pet.id = :petId")
    long countByPetId(@Param("petId") Long petId);
}
