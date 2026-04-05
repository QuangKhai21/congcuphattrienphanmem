package com.db7.j2ee_quanlythucung.service;

import com.db7.j2ee_quanlythucung.entity.LostPetReport;
import com.db7.j2ee_quanlythucung.entity.LostPetReport.ReportStatus;
import com.db7.j2ee_quanlythucung.entity.Pet;
import com.db7.j2ee_quanlythucung.entity.User;
import com.db7.j2ee_quanlythucung.repository.LostPetReportRepository;
import com.db7.j2ee_quanlythucung.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LostPetService {

    private final LostPetReportRepository lostPetReportRepository;
    private final PetRepository petRepository;

    @Transactional(readOnly = true)
    public Optional<LostPetReport> findById(Long id) {
        return lostPetReportRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Page<LostPetReport> findAll(Pageable pageable) {
        return lostPetReportRepository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Page<LostPetReport> findByStatus(ReportStatus status, Pageable pageable) {
        return lostPetReportRepository.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public Page<LostPetReport> findByReporter(Long reporterId, Pageable pageable) {
        return lostPetReportRepository.findByReporterId(reporterId, pageable);
    }

    @Transactional(readOnly = true)
    public List<LostPetReport> findActiveReports() {
        return lostPetReportRepository.findActiveReports();
    }

    @Transactional(readOnly = true)
    public List<LostPetReport> searchActiveReports(String keyword) {
        return lostPetReportRepository.searchActiveReports(keyword);
    }

    @Transactional(readOnly = true)
    public List<LostPetReport> findActiveReportsInArea(Double centerLat, Double centerLng, Double radiusKm) {
        double latDelta = radiusKm / 111.0;
        double lngDelta = radiusKm / (111.0 * Math.cos(Math.toRadians(centerLat)));
        return lostPetReportRepository.findActiveReportsInArea(
                centerLat - latDelta, centerLat + latDelta,
                centerLng - lngDelta, centerLng + lngDelta
        );
    }

    @Transactional
    public LostPetReport createReport(Long petId, LostPetReport report, User reporter) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thú cưng"));
        report.setPet(pet);
        report.setReporter(reporter);
        report.setStatus(ReportStatus.ACTIVE);
        return lostPetReportRepository.save(report);
    }

    @Transactional
    public LostPetReport updateReport(Long id, LostPetReport updatedReport) {
        LostPetReport report = lostPetReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy báo cáo"));
        
        if (updatedReport.getLastSeenLocation() != null) {
            report.setLastSeenLocation(updatedReport.getLastSeenLocation());
        }
        if (updatedReport.getLastSeenLat() != null) {
            report.setLastSeenLat(updatedReport.getLastSeenLat());
        }
        if (updatedReport.getLastSeenLng() != null) {
            report.setLastSeenLng(updatedReport.getLastSeenLng());
        }
        if (updatedReport.getDescription() != null) {
            report.setDescription(updatedReport.getDescription());
        }
        if (updatedReport.getReward() != null) {
            report.setReward(updatedReport.getReward());
        }
        if (updatedReport.getContactPhone() != null) {
            report.setContactPhone(updatedReport.getContactPhone());
        }
        if (updatedReport.getContactable() != null) {
            report.setContactable(updatedReport.getContactable());
        }
        
        return lostPetReportRepository.save(report);
    }

    @Transactional
    public LostPetReport markAsFound(Long id, String notes) {
        LostPetReport report = lostPetReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy báo cáo"));
        report.setStatus(ReportStatus.FOUND);
        report.setResolvedAt(LocalDateTime.now());
        report.setResolvedNotes(notes);
        return lostPetReportRepository.save(report);
    }

    @Transactional
    public LostPetReport closeReport(Long id, String notes) {
        LostPetReport report = lostPetReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy báo cáo"));
        report.setStatus(ReportStatus.CLOSED);
        report.setResolvedAt(LocalDateTime.now());
        report.setResolvedNotes(notes);
        return lostPetReportRepository.save(report);
    }

    @Transactional
    public void deleteReport(Long id) {
        lostPetReportRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public long countActiveReports() {
        return lostPetReportRepository.countByStatus(ReportStatus.ACTIVE);
    }
}
