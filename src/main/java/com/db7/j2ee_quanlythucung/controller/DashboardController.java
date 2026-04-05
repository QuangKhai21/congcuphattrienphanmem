package com.db7.j2ee_quanlythucung.controller;

import com.db7.j2ee_quanlythucung.entity.Appointment;
import com.db7.j2ee_quanlythucung.entity.Pet;
import com.db7.j2ee_quanlythucung.entity.PetCategory;
import com.db7.j2ee_quanlythucung.entity.Vaccination;
import com.db7.j2ee_quanlythucung.repository.*;
import com.db7.j2ee_quanlythucung.security.UserDetailsImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Controller
@Slf4j
@RequiredArgsConstructor
public class DashboardController {

    private final PetRepository petRepository;
    private final AppointmentRepository appointmentRepository;
    private final UserRepository userRepository;
    private final PetCategoryRepository petCategoryRepository;
    private final VaccinationRepository vaccinationRepository;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetailsImpl userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        Long userId = userDetails.getUser().getId();
        boolean isAdminOrManager = userDetails.getUser().getRoles().stream()
                .anyMatch(r -> r.getName().name().startsWith("ROLE_ADMIN") || r.getName().name().startsWith("ROLE_MANAGER"));

        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);
        LocalDate nextWeek = LocalDate.now().plusDays(7);

        if (isAdminOrManager) {
            // Admin/Manager Dashboard - All data
            model.addAttribute("totalPets", petRepository.countAllPets());
            model.addAttribute("totalAppointments", appointmentRepository.countAllAppointments());
            model.addAttribute("totalUsers", userRepository.countAllUsers());
            model.addAttribute("activeUsers", userRepository.countActiveUsers());

            // Today's appointments - SINGLE query with JOIN FETCH
            List<Appointment> todayAppts = appointmentRepository.findByDateRangeWithPetAndOwner(startOfDay, endOfDay);
            model.addAttribute("todayAppointments", todayAppts.size());
            model.addAttribute("todayAppointmentList", todayAppts);

            // Pet distribution - OPTIMIZED: 1 count query per category (not N separate queries)
            List<PetCategory> categories = petCategoryRepository.findAll();
            Map<String, Long> petDistribution = new LinkedHashMap<>();
            for (PetCategory cat : categories) {
                petDistribution.put(cat.getName(), petRepository.countByCategoryId(cat.getId()));
            }
            model.addAttribute("petDistribution", petDistribution);

            // Recent pets - repository already optimized
            model.addAttribute("recentPets", petRepository.findTop5ByOrderByCreatedAtDesc());

            // Upcoming vaccinations - SINGLE query with JOIN FETCH
            List<Vaccination> upcomingVax = vaccinationRepository.findByNextDueDateBetweenWithPetAndOwner(LocalDate.now(), nextWeek);
            model.addAttribute("upcomingVaccinations", upcomingVax);

            // Monthly appointments (toàn hệ thống)
            model.addAttribute("monthlyAppointments", getMonthlyAppointmentStats(null));

        } else {
            // Customer Dashboard - Only their data
            model.addAttribute("totalPets", petRepository.countByOwnerId(userId));
            model.addAttribute("totalAppointments", appointmentRepository.countByPetOwnerId(userId));

            // Today's appointments - SINGLE query, owner filter done in DB
            List<Appointment> todayAppts = appointmentRepository
                    .findByDateRangeAndOwnerWithPetAndOwner(startOfDay, endOfDay, userId);
            model.addAttribute("todayAppointments", todayAppts.size());
            model.addAttribute("todayAppointmentList", todayAppts);

            // Pet distribution for this user - OPTIMIZED: single aggregation query
            List<Pet> userPets = petRepository.findByOwnerId(userId);
            Map<String, Long> petDistribution = userPets.stream()
                    .collect(java.util.stream.Collectors.groupingBy(
                            p -> p.getCategory() != null ? p.getCategory().getName() : "Khác",
                            java.util.stream.Collectors.counting()));
            model.addAttribute("petDistribution", petDistribution);

            // Upcoming vaccinations - SINGLE query with owner filter
            List<Vaccination> upcomingVax = vaccinationRepository
                    .findByNextDueDateBetweenAndOwnerWithPetAndOwner(LocalDate.now(), nextWeek, userId);
            model.addAttribute("upcomingVaccinations", upcomingVax);

            // Recent pets
            model.addAttribute("recentPets", petRepository.findByOwnerIdOrderByCreatedAtDesc(userId).stream().limit(5).toList());

            // Lịch hẹn theo tháng — chỉ lịch của thú cưng thuộc user (trước đây thiếu → biểu đồ trống)
            model.addAttribute("monthlyAppointments", getMonthlyAppointmentStats(userId));
        }

        model.addAttribute("isAdminOrManager", isAdminOrManager);

        return "dashboard";
    }

    /**
     * Số lịch hẹn theo từng tháng trong 6 tháng gần nhất.
     * @param ownerId null = tất cả (admin); có giá trị = chỉ lịch của thú cưng thuộc chủ đó.
     */
    private Map<String, Integer> getMonthlyAppointmentStats(Long ownerId) {
        Map<String, Integer> stats = new LinkedHashMap<>();
        LocalDate now = LocalDate.now();

        for (int i = 5; i >= 0; i--) {
            LocalDate month = now.minusMonths(i);
            String monthName = month.format(java.time.format.DateTimeFormatter.ofPattern("MM/yyyy"));
            LocalDateTime startOfMonth = month.withDayOfMonth(1).atStartOfDay();
            LocalDateTime endOfMonth = month.withDayOfMonth(month.lengthOfMonth()).atTime(LocalTime.MAX);
            List<Appointment> appointments = ownerId == null
                    ? appointmentRepository.findByDateRange(startOfMonth, endOfMonth)
                    : appointmentRepository.findByDateRangeAndOwnerWithPetAndOwner(startOfMonth, endOfMonth, ownerId);
            stats.put(monthName, appointments.size());
        }

        return stats;
    }
}
