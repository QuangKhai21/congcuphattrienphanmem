package com.db7.j2ee_quanlythucung.service;

import com.db7.j2ee_quanlythucung.entity.*;
import com.db7.j2ee_quanlythucung.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HealthService {

    private final MedicalRecordRepository medicalRecordRepository;
    private final VaccinationRepository vaccinationRepository;
    private final WeightRecordRepository weightRecordRepository;
    private final PetRepository petRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;

    // ==================== Medical Records ====================
    @Transactional(readOnly = true)
    public List<MedicalRecord> getMedicalRecordsByPetId(Long petId) {
        return medicalRecordRepository.findByPetId(petId);
    }

    @Transactional(readOnly = true)
    public Optional<MedicalRecord> getMedicalRecordById(Long id) {
        return medicalRecordRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public MedicalRecord getLatestMedicalRecord(Long petId) {
        return medicalRecordRepository.findTop1ByPetIdOrderByRecordDateDesc(petId).orElse(null);
    }

    @Transactional
    public MedicalRecord saveMedicalRecord(MedicalRecord record) {
        return medicalRecordRepository.save(record);
    }

    @Transactional
    public void deleteMedicalRecord(Long id) {
        medicalRecordRepository.deleteById(id);
    }

    // ==================== Vaccinations ====================
    @Transactional(readOnly = true)
    public List<Vaccination> getVaccinationsByPetId(Long petId) {
        return vaccinationRepository.findByPetIdOrderByVaccinationDateDesc(petId);
    }

    @Transactional(readOnly = true)
    public Optional<Vaccination> getVaccinationById(Long id) {
        return vaccinationRepository.findById(id);
    }

    @Transactional
    public Vaccination saveVaccination(Vaccination vaccination) {
        return vaccinationRepository.save(vaccination);
    }

    @Transactional
    public void deleteVaccination(Long id) {
        vaccinationRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<Vaccination> getUpcomingVaccinationReminders(int days) {
        LocalDate reminderDate = LocalDate.now().plusDays(days);
        return vaccinationRepository.findUpcomingReminders(reminderDate);
    }

    @Transactional
    public List<Vaccination> markReminderSent(List<Vaccination> vaccinations) {
        vaccinations.forEach(v -> v.setReminderSent(true));
        return vaccinationRepository.saveAll(vaccinations);
    }

    // ==================== Weight Records ====================
    @Transactional(readOnly = true)
    public List<WeightRecord> getWeightRecordsByPetId(Long petId) {
        return weightRecordRepository.findByPetIdOrderByRecordDateAsc(petId);
    }

    /**
     * Chỉ recordDate + weight + notes + id — dùng cho th:inline JS / Chart,
     * tránh serialize entity chain (pet → appointments → Pet → … → StackOverflow).
     */
    @Transactional(readOnly = true)
    public List<Map<String, String>> getWeightRecordsFlat(Long petId) {
        return weightRecordRepository.findByPetIdOrderByRecordDateAsc(petId)
                .stream()
                .map(w -> {
                    Map<String, String> m = new LinkedHashMap<>();
                    m.put("id",      w.getId()      != null ? w.getId().toString()      : "");
                    m.put("weight",  w.getWeight()  != null ? w.getWeight()            : "");
                    m.put("recordDate", w.getRecordDate() != null ? w.getRecordDate().toString() : "");
                    m.put("notes",   w.getNotes()   != null ? w.getNotes()             : "");
                    return m;
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public Optional<WeightRecord> getWeightRecordById(Long id) {
        return weightRecordRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public WeightRecord getLatestWeightRecord(Long petId) {
        return weightRecordRepository.findTop1ByPetIdOrderByRecordDateDesc(petId).orElse(null);
    }

    @Transactional
    public WeightRecord saveWeightRecord(WeightRecord record) {
        return weightRecordRepository.save(record);
    }

    @Transactional
    public void deleteWeightRecord(Long id) {
        weightRecordRepository.deleteById(id);
    }

    // ==================== Statistics ====================
    @Transactional(readOnly = true)
    public long countMedicalRecords(Long petId) {
        return medicalRecordRepository.countByPetId(petId);
    }

    @Transactional(readOnly = true)
    public long countVaccinations(Long petId) {
        return vaccinationRepository.countByPetId(petId);
    }

    @Transactional(readOnly = true)
    public long countWeightRecords(Long petId) {
        return weightRecordRepository.countByPetId(petId);
    }

    // ==================== Notifications ====================
    @Transactional(readOnly = true)
    public List<Vaccination> getVaccinationReminders(Long userId) {
        LocalDate today = LocalDate.now();
        LocalDate nextWeek = today.plusDays(7);
        return vaccinationRepository.findByNextDueDateBetween(today, nextWeek);
    }

    @Transactional(readOnly = true)
    public List<Appointment> getTodayAppointments() {
        return appointmentRepository.findByDateRange(
            java.time.LocalDateTime.now().toLocalDate().atStartOfDay(),
            java.time.LocalDateTime.now().toLocalDate().atTime(23, 59, 59)
        );
    }

    @Transactional(readOnly = true)
    public long countTodayAppointments() {
        return getTodayAppointments().size();
    }

    @Transactional(readOnly = true)
    public long countUpcomingVaccinationReminders() {
        return getVaccinationReminders(null).size();
    }

    // ==================== Pet Health Summary ====================
    @Transactional(readOnly = true)
    public PetHealthSummary getPetHealthSummary(Long petId) {
        List<MedicalRecord> medicalRecords = medicalRecordRepository.findByPetId(petId);
        List<Vaccination> vaccinations = vaccinationRepository.findByPetIdOrderByVaccinationDateDesc(petId);
        List<WeightRecord> weightRecords = weightRecordRepository.findByPetIdOrderByRecordDateAsc(petId);

        List<Map<String, String>> weightChartData = new ArrayList<>();
        for (WeightRecord w : weightRecords) {
            Map<String, String> row = new LinkedHashMap<>();
            row.put("recordDate", w.getRecordDate() != null ? w.getRecordDate().toString() : "");
            row.put("weight", w.getWeight() != null ? w.getWeight() : "0");
            weightChartData.add(row);
        }

        long totalAppointments = appointmentRepository.countByPetId(petId);

        return PetHealthSummary.builder()
                .medicalRecords(medicalRecords)
                .vaccinations(vaccinations)
                .weightRecords(weightRecords)
                .weightChartData(weightChartData)
                .latestMedicalRecord(medicalRecords.isEmpty() ? null : medicalRecords.get(0))
                .latestVaccination(vaccinations.isEmpty() ? null : vaccinations.get(0))
                .latestWeightRecord(weightRecords.isEmpty() ? null : weightRecords.get(weightRecords.size() - 1))
                .totalMedicalRecords((long) medicalRecords.size())
                .totalVaccinations((long) vaccinations.size())
                .totalWeightRecords((long) weightRecords.size())
                .totalAppointments(totalAppointments)
                .build();
    }

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class PetHealthSummary {
        private List<MedicalRecord> medicalRecords;
        private List<Vaccination> vaccinations;
        private List<WeightRecord> weightRecords;
        /** Chỉ recordDate + weight — dùng cho Chart.js, tránh th:inline serialize entity (pet lazy → lỗi trang trắng). */
        private List<Map<String, String>> weightChartData;
        private MedicalRecord latestMedicalRecord;
        private Vaccination latestVaccination;
        private WeightRecord latestWeightRecord;
        private Long totalMedicalRecords;
        private Long totalVaccinations;
        private Long totalWeightRecords;
        private Long totalAppointments;
    }
}
