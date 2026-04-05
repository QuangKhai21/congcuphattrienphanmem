package com.db7.j2ee_quanlythucung.controller;

import com.db7.j2ee_quanlythucung.entity.*;
import com.db7.j2ee_quanlythucung.entity.HealthMetric.MetricType;
import com.db7.j2ee_quanlythucung.security.UserDetailsImpl;
import com.db7.j2ee_quanlythucung.service.AIHealthAssessmentService;
import com.db7.j2ee_quanlythucung.service.HealthMonitoringService;
import com.db7.j2ee_quanlythucung.service.PetService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

    private final HealthMonitoringService healthService;
    private final AIHealthAssessmentService aiService;
    private final PetService petService;

    private boolean isAdminOrManager(User user) {
        return user.getRoles().stream()
                .anyMatch(r -> r.getName().name().equals("ROLE_ADMIN")
                        || r.getName().name().equals("ROLE_MANAGER"));
    }

    private void checkPetAccess(Long petId, UserDetailsImpl userDetails) {
        Pet pet = petService.findById(petId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thú cưng"));
        User currentUser = userDetails.getUser();
        if (!isAdminOrManager(currentUser) && !pet.getOwner().getId().equals(currentUser.getId())) {
            throw new SecurityException("Bạn không có quyền truy cập thú cưng này");
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetailsImpl userDetails, Model model) {
        User user = userDetails.getUser();
        List<Pet> pets;

        if (isAdminOrManager(user)) {
            pets = petService.findAll(PageRequest.of(0, 50)).getContent();
        } else {
            pets = petService.findByOwnerId(user.getId(), PageRequest.of(0, 50)).getContent();
        }

        model.addAttribute("pets", pets);

        if (!pets.isEmpty()) {
            Map<Long, AIHealthAssessmentService.HealthAssessmentResult> assessments = new HashMap<>();
            Map<Long, Integer> petAges = new HashMap<>();
            List<HealthMetric> allAlerts = healthService.findAlertsByOwner(user.getId());

            for (Pet pet : pets) {
                assessments.put(pet.getId(), aiService.assessPetHealth(pet));
                if (pet.getDateOfBirth() != null) {
                    petAges.put(pet.getId(), Period.between(pet.getDateOfBirth(), LocalDate.now()).getYears());
                }
            }

            model.addAttribute("firstPetId", pets.get(0).getId());
            model.addAttribute("assessments", assessments);
            model.addAttribute("petAges", petAges);
            model.addAttribute("alerts", allAlerts);
            model.addAttribute("alertCount", allAlerts.size());
        } else {
            model.addAttribute("alertCount", 0);
        }

        return "health/dashboard";
    }

    @GetMapping("/pet/{petId}")
    public String petHealth(@PathVariable Long petId,
                            @RequestParam(defaultValue = "0") int page,
                            @AuthenticationPrincipal UserDetailsImpl userDetails,
                            Model model) {
        checkPetAccess(petId, userDetails);
        Pet pet = petService.findById(petId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thú cưng"));

        Page<HealthMetric> metrics = healthService.findByPet(petId, page, 20);
        AIHealthAssessmentService.HealthAssessmentResult assessment = aiService.assessPetHealth(pet);
        List<HealthMetric> latestMetrics = healthService.findLatestByPet(petId, 10);
        List<SeizureLog> recentSeizures = healthService.findRecentSeizures(petId, 30);
        HealthMonitoringService.SeizureStatistics seizureStats = healthService.getSeizureStatistics(petId, 30);
        NutritionPlan activePlan = healthService.findActivePlan(petId).orElse(null);

        model.addAttribute("pet", pet);
        model.addAttribute("metrics", metrics.getContent());
        model.addAttribute("totalPages", metrics.getTotalPages());
        model.addAttribute("currentPage", page);
        model.addAttribute("assessment", assessment);
        model.addAttribute("latestMetrics", latestMetrics);
        model.addAttribute("recentSeizures", recentSeizures);
        model.addAttribute("seizureStats", seizureStats);
        model.addAttribute("activePlan", activePlan);
        model.addAttribute("metricTypes", MetricType.values());

        return "health/pet-health";
    }

    @GetMapping("/pet/{petId}/assess")
    @ResponseBody
    public AIHealthAssessmentService.HealthAssessmentResult assess(@PathVariable Long petId,
                                                                     @AuthenticationPrincipal UserDetailsImpl userDetails) {
        checkPetAccess(petId, userDetails);
        Pet pet = petService.findById(petId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thú cưng"));
        return aiService.assessPetHealth(pet);
    }

    @GetMapping("/pet/{petId}/trend")
    @ResponseBody
    public AIHealthAssessmentService.HealthTrend trend(@PathVariable Long petId,
                                                       @RequestParam MetricType type,
                                                       @RequestParam(defaultValue = "30") int days,
                                                       @AuthenticationPrincipal UserDetailsImpl userDetails) {
        checkPetAccess(petId, userDetails);
        Pet pet = petService.findById(petId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thú cưng"));
        return aiService.analyzeTrend(pet, type, days);
    }

    @GetMapping("/pet/{petId}/nutrition")
    @ResponseBody
    public AIHealthAssessmentService.NutritionAdvice nutrition(@PathVariable Long petId,
                                                               @AuthenticationPrincipal UserDetailsImpl userDetails) {
        checkPetAccess(petId, userDetails);
        Pet pet = petService.findById(petId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thú cưng"));
        List<HealthMetric> recentMetrics = healthService.findLatestByPet(petId, 30);
        return aiService.generateNutritionAdvice(pet, recentMetrics);
    }

    @PostMapping("/pet/{petId}/metric")
    public String addMetric(@PathVariable Long petId,
                           @RequestParam MetricType type,
                           @RequestParam Double value,
                           @RequestParam(required = false) String unit,
                           @RequestParam(required = false) String date,
                           @RequestParam(required = false) String notes,
                           @AuthenticationPrincipal UserDetailsImpl userDetails,
                           RedirectAttributes redirectAttributes) {
        try {
            checkPetAccess(petId, userDetails);
            LocalDateTime recordedAt = parseMetricDateTime(date);
            healthService.recordMetric(petId, type, value, unit, recordedAt, notes, userDetails.getUser().getId());
            redirectAttributes.addFlashAttribute("success", "Đã ghi nhận chỉ số: " + type.name());
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/health/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/health/pet/" + petId;
    }

    @PostMapping("/pet/{petId}/seizure")
    public String logSeizure(@PathVariable Long petId,
                             @RequestParam LocalDateTime seizureDate,
                             @RequestParam(required = false) Double duration,
                             @RequestParam(required = false) SeizureLog.SeizureType type,
                             @RequestParam(required = false, defaultValue = "5") Integer severity,
                             @RequestParam(required = false) String symptoms,
                             @RequestParam(required = false) String triggers,
                             @RequestParam(required = false) String treatment,
                             @RequestParam(required = false) String notes,
                             @AuthenticationPrincipal UserDetailsImpl userDetails,
                             RedirectAttributes redirectAttributes) {
        try {
            checkPetAccess(petId, userDetails);
            healthService.logSeizure(petId, seizureDate, duration, type, severity,
                    symptoms, triggers, treatment, notes);
            redirectAttributes.addFlashAttribute("success", "Đã ghi nhận cơn động kinh!");
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/health/dashboard";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/health/pet/" + petId;
    }

    @PostMapping("/metric/{id}/delete")
    public String deleteMetric(@PathVariable Long id,
                              @RequestParam Long petId,
                              @AuthenticationPrincipal UserDetailsImpl userDetails,
                              RedirectAttributes redirectAttributes) {
        try {
            checkPetAccess(petId, userDetails);
            healthService.deleteMetric(id);
            redirectAttributes.addFlashAttribute("success", "Đã xóa chỉ số");
        } catch (SecurityException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/health/dashboard";
        }
        return "redirect:/health/pet/" + petId;
    }

    @GetMapping("/pet/{petId}/weight-chart")
    @ResponseBody
    public List<Map<String, Object>> weightChart(@PathVariable Long petId,
                                                  @RequestParam(defaultValue = "30") int days,
                                                  @AuthenticationPrincipal UserDetailsImpl userDetails) {
        checkPetAccess(petId, userDetails);
        Pet pet = petService.findById(petId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thú cưng"));
        AIHealthAssessmentService.HealthTrend trend = aiService.analyzeTrend(pet, MetricType.WEIGHT, days);
        return List.of(
                Map.of("type", "trend", "data", trend),
                Map.of("type", "latest", "value", healthService.findLatestByPetAndType(petId, MetricType.WEIGHT)
                        .map(m -> m.getValue()).orElse(0.0))
        );
    }

    /** datetime-local có thể để trống; trình duyệt gửi có/không có giây. */
    private static LocalDateTime parseMetricDateTime(String date) {
        if (date == null || date.isBlank()) {
            return LocalDateTime.now();
        }
        String s = date.trim();
        DateTimeFormatter[] formatters = {
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"),
                DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"),
                DateTimeFormatter.ISO_LOCAL_DATE_TIME
        };
        for (DateTimeFormatter fmt : formatters) {
            try {
                return LocalDateTime.parse(s, fmt);
            } catch (DateTimeParseException ignored) {
                // thử định dạng khác
            }
        }
        throw new IllegalArgumentException("Định dạng ngày giờ không hợp lệ: " + date);
    }
}
