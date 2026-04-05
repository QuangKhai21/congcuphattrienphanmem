package com.db7.j2ee_quanlythucung.controller;

import com.db7.j2ee_quanlythucung.entity.*;
import com.db7.j2ee_quanlythucung.repository.*;
import com.db7.j2ee_quanlythucung.security.UserDetailsImpl;
import com.db7.j2ee_quanlythucung.service.AppointmentExcelService;
import com.db7.j2ee_quanlythucung.service.MedicalRecordExcelService;
import com.db7.j2ee_quanlythucung.service.PetExcelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final PetRepository petRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final AppointmentRepository appointmentRepository;
    private final VaccinationRepository vaccinationRepository;
    private final UserRepository userRepository;
    private final PetExcelService petExcelService;
    private final AppointmentExcelService appointmentExcelService;
    private final MedicalRecordExcelService medicalRecordExcelService;

    @GetMapping
    public String reportIndex(@AuthenticationPrincipal UserDetailsImpl userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        
        boolean isAdminOrManager = userDetails.getUser().getRoles().stream()
                .anyMatch(r -> r.getName().name().startsWith("ROLE_ADMIN") || r.getName().name().startsWith("ROLE_MANAGER"));
        
        model.addAttribute("isAdminOrManager", isAdminOrManager);
        return "reports/index";
    }

    // ==================== Pet Report ====================
    @GetMapping("/pets")
    public String petReport(@AuthenticationPrincipal UserDetailsImpl userDetails,
                           @RequestParam(required = false) String format,
                           Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        List<Pet> pets = getPetsForUser(userDetails);

        LocalDate today = LocalDate.now();
        long petsCreatedToday = pets.stream()
                .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().toLocalDate().equals(today))
                .count();
        long distinctCategoryCount = pets.stream()
                .map(p -> p.getCategory() != null ? p.getCategory().getId() : null)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();

        String reportDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));

        model.addAttribute("pets", pets);
        model.addAttribute("totalPets", pets.size());
        model.addAttribute("petsCreatedToday", petsCreatedToday);
        model.addAttribute("distinctCategoryCount", distinctCategoryCount);
        model.addAttribute("reportDate", reportDate);

        return "reports/pets";
    }

    @GetMapping("/pets/excel")
    @ResponseBody
    public ResponseEntity<byte[]> petReportExcel(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        List<Pet> pets = getPetsForUser(userDetails);
        String reportDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        try {
            byte[] excelData = petExcelService.exportPets(pets, reportDate);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"BaoCaoThuCung_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private List<Pet> getPetsForUser(UserDetailsImpl userDetails) {
        if (userDetails.getUser().getRoles().stream()
                .anyMatch(r -> r.getName().name().startsWith("ROLE_ADMIN") || r.getName().name().startsWith("ROLE_MANAGER"))) {
            return petRepository.findAll();
        } else {
            return petRepository.findByOwnerId(userDetails.getUser().getId());
        }
    }

    // ==================== Medical Records Report ====================
    @GetMapping("/medical-records")
    public String medicalRecordsList(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                    Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        List<Pet> pets = getPetsForUser(userDetails);
        model.addAttribute("pets", pets);
        return "reports/medical_records";
    }

    @GetMapping("/medical-records/{petId}")
    public String medicalRecordsReport(@PathVariable Long petId,
                                      @AuthenticationPrincipal UserDetailsImpl userDetails,
                                      RedirectAttributes redirectAttributes,
                                      Model model) {
        Pet pet = petRepository.findById(petId).orElse(null);
        if (pet == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy thú cưng");
            return "redirect:/reports";
        }
        User currentUser = userDetails.getUser();
        boolean isAdminOrManager = currentUser.getRoles().stream()
                .anyMatch(r -> r.getName().name().startsWith("ROLE_ADMIN") || r.getName().name().startsWith("ROLE_MANAGER"));
        if (!isAdminOrManager && !pet.getOwner().getId().equals(currentUser.getId())) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền xem hồ sơ này");
            return "redirect:/reports";
        }
        List<MedicalRecord> records = medicalRecordRepository.findByPetId(petId);

        model.addAttribute("pet", pet);
        model.addAttribute("records", records);
        model.addAttribute("totalRecords", records.size());
        model.addAttribute("reportDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        return "reports/medical_records_detail";
    }

    @GetMapping("/medical-records/{petId}/excel")
    @ResponseBody
    public ResponseEntity<byte[]> medicalRecordsExcel(@PathVariable Long petId,
                                                      @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        Pet pet = petRepository.findById(petId).orElse(null);
        if (pet == null) {
            return ResponseEntity.notFound().build();
        }
        // Check access
        User currentUser = userDetails.getUser();
        boolean isAdminOrManager = currentUser.getRoles().stream()
                .anyMatch(r -> r.getName().name().startsWith("ROLE_ADMIN") || r.getName().name().startsWith("ROLE_MANAGER"));
        if (!isAdminOrManager && !pet.getOwner().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(403).build();
        }

        List<MedicalRecord> records = medicalRecordRepository.findByPetId(petId);
        String reportDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        try {
            byte[] excelData = medicalRecordExcelService.exportMedicalRecords(pet, records, reportDate);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"BaoCaoBenhAn_" + pet.getName().replaceAll("\\s+", "_") + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    // ==================== Appointment Report ====================
    @GetMapping("/appointments")
    public String appointmentReport(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                   @RequestParam(required = false) String startDate,
                                   @RequestParam(required = false) String endDate,
                                   Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        List<Appointment> appointments = getAppointmentsForUser(userDetails, startDate, endDate);

        LocalDateTime start = startDate != null
                ? LocalDate.parse(startDate).atStartOfDay()
                : LocalDate.now().minusMonths(1).atStartOfDay();
        LocalDateTime end = endDate != null
                ? LocalDate.parse(endDate).atTime(LocalTime.MAX)
                : LocalDateTime.now();

        model.addAttribute("appointments", appointments);
        model.addAttribute("totalAppointments", appointments.size());
        model.addAttribute("startDate", start.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        model.addAttribute("endDate", end.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        model.addAttribute("reportDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        return "reports/appointments";
    }

    @GetMapping("/appointments/excel")
    @ResponseBody
    public ResponseEntity<byte[]> appointmentReportExcel(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                                        @RequestParam(required = false) String startDate,
                                                        @RequestParam(required = false) String endDate) {
        if (userDetails == null) {
            return ResponseEntity.status(401).build();
        }
        List<Appointment> appointments = getAppointmentsForUser(userDetails, startDate, endDate);
        LocalDateTime start = startDate != null
                ? LocalDate.parse(startDate).atStartOfDay()
                : LocalDate.now().minusMonths(1).atStartOfDay();
        LocalDateTime end = endDate != null
                ? LocalDate.parse(endDate).atTime(LocalTime.MAX)
                : LocalDateTime.now();
        String sDate = start.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String eDate = end.toLocalDate().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
        String reportDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        try {
            byte[] excelData = appointmentExcelService.exportAppointments(appointments, sDate, eDate, reportDate);
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename=\"BaoCaoLichHen_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".xlsx\"")
                    .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                    .body(excelData);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private List<Appointment> getAppointmentsForUser(UserDetailsImpl userDetails, String startDate, String endDate) {
        LocalDateTime start = startDate != null
                ? LocalDate.parse(startDate).atStartOfDay()
                : LocalDate.now().minusMonths(1).atStartOfDay();
        LocalDateTime end = endDate != null
                ? LocalDate.parse(endDate).atTime(LocalTime.MAX)
                : LocalDateTime.now();

        List<Appointment> appointments = appointmentRepository.findByDateRange(start, end);

        if (!userDetails.getUser().getRoles().stream()
                .anyMatch(r -> r.getName().name().startsWith("ROLE_ADMIN") || r.getName().name().startsWith("ROLE_MANAGER"))) {
            final Long ownerId = userDetails.getUser().getId();
            appointments = appointments.stream()
                    .filter(a -> a.getPet() != null && a.getPet().getOwner() != null && a.getPet().getOwner().getId().equals(ownerId))
                    .toList();
        }
        return appointments;
    }

    // ==================== Statistics Report ====================
    @GetMapping("/statistics")
    public String statisticsReport(@AuthenticationPrincipal UserDetailsImpl userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        boolean isAdminOrManager = userDetails.getUser().getRoles().stream()
                .anyMatch(r -> r.getName().name().startsWith("ROLE_ADMIN") || r.getName().name().startsWith("ROLE_MANAGER"));

        if (!isAdminOrManager) {
            return "redirect:/dashboard";
        }

        // Pet statistics
        long totalPets = petRepository.countAllPets();
        Map<String, Long> petStatusStats = new HashMap<>();
        petStatusStats.put("Đang nuôi", petRepository.countByStatus(Pet.PetStatus.ACTIVE));
        petStatusStats.put("Đã mất", petRepository.countByStatus(Pet.PetStatus.PASSED));
        petStatusStats.put("Đã chuyển", petRepository.countByStatus(Pet.PetStatus.TRANSFERRED));

        // User statistics
        long totalUsers = userRepository.countAllUsers();
        long activeUsers = userRepository.countActiveUsers();

        // Appointment statistics
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();
        LocalDateTime endOfMonth = LocalDate.now().atTime(LocalTime.MAX);
        List<Appointment> monthAppointments = appointmentRepository.findByDateRange(startOfMonth, endOfMonth);
        
        Map<String, Long> appointmentStatusStats = new HashMap<>();
        appointmentStatusStats.put("Hoàn thành", monthAppointments.stream().filter(a -> a.getStatus() == Appointment.AppointmentStatus.COMPLETED).count());
        appointmentStatusStats.put("Đã hủy", monthAppointments.stream().filter(a -> a.getStatus() == Appointment.AppointmentStatus.CANCELLED).count());
        appointmentStatusStats.put("Đang xử lý", monthAppointments.stream().filter(a -> a.getStatus() == Appointment.AppointmentStatus.IN_PROGRESS).count());
        appointmentStatusStats.put("Đã đặt", monthAppointments.stream().filter(a -> a.getStatus() == Appointment.AppointmentStatus.SCHEDULED).count());

        model.addAttribute("totalPets", totalPets);
        model.addAttribute("petStatusStats", petStatusStats);
        model.addAttribute("totalUsers", totalUsers);
        model.addAttribute("activeUsers", activeUsers);
        model.addAttribute("monthAppointments", monthAppointments.size());
        model.addAttribute("appointmentStatusStats", appointmentStatusStats);
        model.addAttribute("reportDate", LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        return "reports/statistics";
    }
}
