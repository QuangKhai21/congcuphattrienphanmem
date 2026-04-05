package com.db7.j2ee_quanlythucung.repository;

import com.db7.j2ee_quanlythucung.entity.SeizureLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SeizureLogRepository extends JpaRepository<SeizureLog, Long> {

    Page<SeizureLog> findByPetIdOrderBySeizureDateDesc(Long petId, Pageable pageable);

    List<SeizureLog> findByPetIdOrderBySeizureDateDesc(Long petId);

    @Query("SELECT s FROM SeizureLog s WHERE s.pet.id = :petId AND s.seizureDate >= :since ORDER BY s.seizureDate DESC")
    List<SeizureLog> findByPetIdSince(@Param("petId") Long petId, @Param("since") LocalDateTime since);

    @Query("SELECT COUNT(s) FROM SeizureLog s WHERE s.pet.id = :petId AND s.seizureDate >= :since")
    Long countSeizuresSince(@Param("petId") Long petId, @Param("since") LocalDateTime since);

    @Query("SELECT AVG(s.severityScore) FROM SeizureLog s WHERE s.pet.id = :petId AND s.seizureDate >= :since")
    Double getAverageSeveritySince(@Param("petId") Long petId, @Param("since") LocalDateTime since);
}
