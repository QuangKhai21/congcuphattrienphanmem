package com.db7.j2ee_quanlythucung.repository;

import com.db7.j2ee_quanlythucung.entity.LostPetReport;
import com.db7.j2ee_quanlythucung.entity.LostPetReport.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LostPetReportRepository extends JpaRepository<LostPetReport, Long> {

    Page<LostPetReport> findByStatus(ReportStatus status, Pageable pageable);

    Page<LostPetReport> findByReporterId(Long reporterId, Pageable pageable);

    Page<LostPetReport> findByPetOwnerId(Long ownerId, Pageable pageable);

    @Query("SELECT l FROM LostPetReport l WHERE l.status = 'ACTIVE' ORDER BY l.createdAt DESC")
    List<LostPetReport> findActiveReports();

    @Query("SELECT l FROM LostPetReport l WHERE l.status = 'ACTIVE' AND " +
           "(l.lastSeenLat BETWEEN :minLat AND :maxLat) AND " +
           "(l.lastSeenLng BETWEEN :minLng AND :maxLng)")
    List<LostPetReport> findActiveReportsInArea(
            @Param("minLat") Double minLat,
            @Param("maxLat") Double maxLat,
            @Param("minLng") Double minLng,
            @Param("maxLng") Double maxLng);

    @Query("SELECT l FROM LostPetReport l WHERE l.status = 'ACTIVE' " +
           "AND (LOWER(l.pet.name) LIKE LOWER(CONCAT('%',:keyword,'%')) " +
           "OR LOWER(l.description) LIKE LOWER(CONCAT('%',:keyword,'%')))")
    List<LostPetReport> searchActiveReports(@Param("keyword") String keyword);

    long countByStatus(ReportStatus status);
}
