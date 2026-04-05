package com.db7.j2ee_quanlythucung.repository;

import com.db7.j2ee_quanlythucung.entity.HealthMetric;
import com.db7.j2ee_quanlythucung.entity.HealthMetric.MetricType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface HealthMetricRepository extends JpaRepository<HealthMetric, Long> {

    Page<HealthMetric> findByPetId(Long petId, Pageable pageable);

    List<HealthMetric> findByPetIdOrderByRecordedAtDesc(Long petId);

    Page<HealthMetric> findByPetIdAndMetricType(Long petId, MetricType metricType, Pageable pageable);

    @Query("SELECT h FROM HealthMetric h WHERE h.pet.id = :petId AND h.metricType = :type " +
           "AND h.recordedAt BETWEEN :start AND :end ORDER BY h.recordedAt ASC")
    List<HealthMetric> findByPetAndTypeInDateRange(
            @Param("petId") Long petId,
            @Param("type") MetricType type,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end);

    @Query("SELECT h FROM HealthMetric h WHERE h.pet.id = :petId AND h.alertTriggered = true " +
           "ORDER BY h.recordedAt DESC")
    List<HealthMetric> findAlertsByPetId(@Param("petId") Long petId);

    @Query("SELECT h FROM HealthMetric h WHERE h.pet.owner.id = :ownerId AND h.alertTriggered = true " +
           "ORDER BY h.recordedAt DESC")
    List<HealthMetric> findAlertsByOwnerId(@Param("ownerId") Long ownerId);

    @Query("SELECT AVG(h.value) FROM HealthMetric h WHERE h.pet.id = :petId AND h.metricType = :type")
    Double getAverageByPetAndType(@Param("petId") Long petId, @Param("type") MetricType type);

    @Query("SELECT MAX(h.value) FROM HealthMetric h WHERE h.pet.id = :petId AND h.metricType = :type " +
           "AND h.recordedAt > :since")
    Double getMaxByPetAndTypeSince(@Param("petId") Long petId, @Param("type") MetricType type,
                                   @Param("since") LocalDateTime since);

    @Query("SELECT MIN(h.value) FROM HealthMetric h WHERE h.pet.id = :petId AND h.metricType = :type " +
           "AND h.recordedAt > :since")
    Double getMinByPetAndTypeSince(@Param("petId") Long petId, @Param("type") MetricType type,
                                   @Param("since") LocalDateTime since);

    Optional<HealthMetric> findTopByPetIdAndMetricTypeOrderByRecordedAtDesc(Long petId, MetricType type);
}
