package com.db7.j2ee_quanlythucung.repository;

import com.db7.j2ee_quanlythucung.entity.PetSighting;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PetSightingRepository extends JpaRepository<PetSighting, Long> {

    Page<PetSighting> findByReportId(Long reportId, Pageable pageable);

    List<PetSighting> findByReportIdOrderByCreatedAtDesc(Long reportId);
}
