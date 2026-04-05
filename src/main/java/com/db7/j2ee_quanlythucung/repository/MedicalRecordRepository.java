package com.db7.j2ee_quanlythucung.repository;

import com.db7.j2ee_quanlythucung.entity.MedicalRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {

    @Query("SELECT m FROM MedicalRecord m WHERE m.pet.id = :petId ORDER BY m.recordDate DESC")
    List<MedicalRecord> findByPetId(@Param("petId") Long petId);

    @Query("SELECT m FROM MedicalRecord m WHERE m.pet.owner.id = :ownerId ORDER BY m.recordDate DESC")
    List<MedicalRecord> findByOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT m FROM MedicalRecord m WHERE m.pet.id = :petId AND LOWER(m.diagnosis) LIKE LOWER(CONCAT('%', :keyword, '%')) ORDER BY m.recordDate DESC")
    List<MedicalRecord> searchByKeyword(@Param("petId") Long petId, @Param("keyword") String keyword);

    long countByPetId(Long petId);

    Optional<MedicalRecord> findTop1ByPetIdOrderByRecordDateDesc(Long petId);
}
