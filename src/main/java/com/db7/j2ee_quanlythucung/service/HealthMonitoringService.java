package com.db7.j2ee_quanlythucung.service;

import com.db7.j2ee_quanlythucung.entity.HealthMetric;
import com.db7.j2ee_quanlythucung.entity.HealthMetric.MetricType;
import com.db7.j2ee_quanlythucung.entity.NutritionPlan;
import com.db7.j2ee_quanlythucung.entity.Pet;
import com.db7.j2ee_quanlythucung.entity.SeizureLog;
import com.db7.j2ee_quanlythucung.entity.User;
import com.db7.j2ee_quanlythucung.repository.HealthMetricRepository;
import com.db7.j2ee_quanlythucung.repository.NutritionPlanRepository;
import com.db7.j2ee_quanlythucung.repository.PetRepository;
import com.db7.j2ee_quanlythucung.repository.SeizureLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class HealthMonitoringService {

    private final HealthMetricRepository metricRepository;
    private final SeizureLogRepository seizureLogRepository;
    private final NutritionPlanRepository nutritionPlanRepository;
    private final PetRepository petRepository;
    private final AIHealthAssessmentService aiHealthAssessmentService;

    // ======================== HEALTH METRICS ========================

    @Transactional(readOnly = true)
    public Optional<HealthMetric> findMetricById(Long id) {
        return metricRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Page<HealthMetric> findByPet(Long petId, int page, int size) {
        return metricRepository.findByPetId(petId, PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "recordedAt")));
    }

    @Transactional(readOnly = true)
    public List<HealthMetric> findLatestByPet(Long petId, int limit) {
        return metricRepository.findByPetIdOrderByRecordedAtDesc(petId)
                .stream().limit(limit).toList();
    }

    @Transactional(readOnly = true)
    public Page<HealthMetric> findByPetAndType(Long petId, MetricType type, int page, int size) {
        return metricRepository.findByPetIdAndMetricType(petId, type, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Optional<HealthMetric> findLatestByPetAndType(Long petId, MetricType type) {
        return metricRepository.findTopByPetIdAndMetricTypeOrderByRecordedAtDesc(petId, type);
    }

    @Transactional(readOnly = true)
    public List<HealthMetric> findAlertsByOwner(Long ownerId) {
        return metricRepository.findAlertsByOwnerId(ownerId);
    }

    @Transactional
    public HealthMetric recordMetric(Long petId, MetricType type, Double value, String unit,
                                    LocalDateTime recordedAt, String notes, Long recordedById) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thú cưng"));

        HealthMetric metric = HealthMetric.builder()
                .pet(pet)
                .metricType(type)
                .value(value)
                .unit(unit != null ? unit : getDefaultUnit(type))
                .recordedAt(recordedAt != null ? recordedAt : LocalDateTime.now())
                .notes(notes)
                .alertTriggered(false)
                .build();

        HealthMetric saved = metricRepository.save(metric);

        // Trigger AI analysis
        aiHealthAssessmentService.analyzeAndUpdateMetric(saved);

        return saved;
    }

    @Transactional
    public void deleteMetric(Long id) {
        metricRepository.deleteById(id);
    }

    // ======================== SEIZURE TRACKING ========================

    @Transactional(readOnly = true)
    public Page<SeizureLog> findSeizureLogs(Long petId, int page, int size) {
        return seizureLogRepository.findByPetIdOrderBySeizureDateDesc(petId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public List<SeizureLog> findRecentSeizures(Long petId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        return seizureLogRepository.findByPetIdSince(petId, since);
    }

    @Transactional
    public SeizureLog logSeizure(Long petId, LocalDateTime seizureDate, Double duration,
                                 SeizureLog.SeizureType type, Integer severity,
                                 String symptoms, String triggers, String treatment, String notes) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thú cưng"));

        SeizureLog log = SeizureLog.builder()
                .pet(pet)
                .seizureDate(seizureDate)
                .durationMinutes(duration)
                .type(type != null ? type : SeizureLog.SeizureType.GENERALIZED)
                .severityScore(severity != null ? severity : 5)
                .symptoms(symptoms)
                .triggers(triggers)
                .treatmentGiven(treatment)
                .notes(notes)
                .build();

        return seizureLogRepository.save(log);
    }

    @Transactional(readOnly = true)
    public SeizureStatistics getSeizureStatistics(Long petId, int days) {
        LocalDateTime since = LocalDateTime.now().minusDays(days);
        List<SeizureLog> logs = seizureLogRepository.findByPetIdSince(petId, since);
        Long count = seizureLogRepository.countSeizuresSince(petId, since);
        Double avgSeverity = seizureLogRepository.getAverageSeveritySince(petId, since);

        return new SeizureStatistics(
                count != null ? count.intValue() : 0,
                avgSeverity != null ? avgSeverity : 0.0,
                logs.stream().filter(l -> l.getDurationMinutes() != null).mapToDouble(SeizureLog::getDurationMinutes).average().orElse(0.0),
                logs.stream().filter(l -> l.getSeverityScore() != null).mapToDouble(SeizureLog::getSeverityScore).average().orElse(0.0)
        );
    }

    // ======================== NUTRITION PLANS ========================

    @Transactional(readOnly = true)
    public Page<NutritionPlan> findNutritionPlans(Long petId, int page, int size) {
        return nutritionPlanRepository.findByPetId(petId, PageRequest.of(page, size));
    }

    @Transactional(readOnly = true)
    public Optional<NutritionPlan> findActivePlan(Long petId) {
        return nutritionPlanRepository.findByPetIdAndIsActiveTrueOrderByCreatedAtDesc(petId);
    }

    @Transactional
    public NutritionPlan createNutritionPlan(Long petId, String planName, String dailyMeals,
                                             Double dailyCalories, String foodRecs,
                                             String foodsToAvoid, String supplements, String notes) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thú cưng"));

        NutritionPlan plan = NutritionPlan.builder()
                .pet(pet)
                .planName(planName)
                .dailyMeals(dailyMeals)
                .dailyCalories(dailyCalories)
                .foodRecommendations(foodRecs)
                .foodsToAvoid(foodsToAvoid)
                .supplements(supplements)
                .notes(notes)
                .isActive(true)
                .build();

        return nutritionPlanRepository.save(plan);
    }

    // ======================== HELPERS ========================

    private String getDefaultUnit(MetricType type) {
        return switch (type) {
            case WEIGHT -> "kg";
            case HEIGHT -> "cm";
            case TEMPERATURE -> "°C";
            case HEART_RATE -> "bpm";
            case RESPIRATION -> "lần/phút";
            case WATER_INTAKE -> "ml";
            case MEDICATION_DOSE -> "mg";
            case BLOOD_PRESSURE -> "mmHg";
            case BLOOD_SUGAR -> "mg/dL";
            default -> "";
        };
    }

    public record SeizureStatistics(int seizureCount, double avgSeverity, double avgDuration, double overallSeverity) {}
}
