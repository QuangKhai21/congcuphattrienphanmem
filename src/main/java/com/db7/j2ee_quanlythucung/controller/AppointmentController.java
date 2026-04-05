package com.db7.j2ee_quanlythucung.controller;

import com.db7.j2ee_quanlythucung.entity.Appointment;
import com.db7.j2ee_quanlythucung.entity.Pet;
import com.db7.j2ee_quanlythucung.entity.User;
import com.db7.j2ee_quanlythucung.security.UserDetailsImpl;
import com.db7.j2ee_quanlythucung.service.AppointmentService;
import com.db7.j2ee_quanlythucung.service.PetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final PetService petService;

    private boolean isStaff(User user) {
        return user.getRoles().stream()
                .anyMatch(r -> r.getName().name().equals("ROLE_ADMIN")
                        || r.getName().name().equals("ROLE_MANAGER")
                        || r.getName().name().equals("ROLE_STAFF")
                        || r.getName().name().equals("ROLE_VET"));
    }

    private boolean canModifyAppointment(Appointment apt, User user) {
        if (apt == null) return false;
        if (isStaff(user)) return true;
        if (apt.getPet() == null || apt.getPet().getOwner() == null) return false;
        if (!apt.getPet().getOwner().getId().equals(user.getId())) return false;
        Appointment.AppointmentStatus s = apt.getStatus();
        return s != Appointment.AppointmentStatus.COMPLETED
                && s != Appointment.AppointmentStatus.CANCELLED
                && s != Appointment.AppointmentStatus.NO_SHOW;
    }

    private List<Pet> petsForUser(User user) {
        if (isStaff(user)) {
            return petService.findAll(PageRequest.of(0, 500)).getContent();
        }
        return petService.findByOwnerId(user.getId(), PageRequest.of(0, 200)).getContent();
    }

    @GetMapping
    public String list(@AuthenticationPrincipal UserDetailsImpl userDetails,
                       @RequestParam(defaultValue = "0") int page,
                       Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        User user = userDetails.getUser();

        Page<Appointment> appointmentPage = isStaff(user)
                ? appointmentService.findAll(PageRequest.of(page, 10))
                : appointmentService.findByOwnerId(user.getId(), PageRequest.of(page, 10));

        model.addAttribute("appointments", appointmentPage.getContent());
        model.addAttribute("totalPages", appointmentPage.getTotalPages());
        model.addAttribute("currentPage", page);
        model.addAttribute("isStaff", isStaff(user));
        return "appointments/list";
    }

    @GetMapping("/new")
    public String createForm(@AuthenticationPrincipal UserDetailsImpl userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        model.addAttribute("appointment", new Appointment());
        model.addAttribute("pets", petsForUser(userDetails.getUser()));
        model.addAttribute("types", Appointment.AppointmentType.values());
        model.addAttribute("statuses", Appointment.AppointmentStatus.values());
        model.addAttribute("editMode", false);
        model.addAttribute("isStaff", isStaff(userDetails.getUser()));
        return "appointments/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute Appointment appointment, BindingResult result,
                         @RequestParam Long petId,
                         @RequestParam String date,
                         @RequestParam String time,
                         @AuthenticationPrincipal UserDetailsImpl userDetails,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        if (result.hasErrors()) {
            model.addAttribute("pets", petsForUser(userDetails.getUser()));
            model.addAttribute("types", Appointment.AppointmentType.values());
            model.addAttribute("statuses", Appointment.AppointmentStatus.values());
            model.addAttribute("editMode", false);
            model.addAttribute("isStaff", isStaff(userDetails.getUser()));
            return "appointments/form";
        }

        Pet pet = petService.findById(petId).orElseThrow(
                () -> new RuntimeException("Không tìm thấy thú cưng"));

        User currentUser = userDetails.getUser();
        boolean isStaffUser = isStaff(currentUser);

        if (!isStaffUser && !pet.getOwner().getId().equals(currentUser.getId())) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền đặt lịch cho thú cưng này");
            return "redirect:/appointments";
        }

        try {
            LocalDate d = LocalDate.parse(date);
            LocalTime t = LocalTime.parse(time);
            appointment.setAppointmentDate(LocalDateTime.of(d, t));
        } catch (Exception e) {
            model.addAttribute("error", "Định dạng ngày/giờ không hợp lệ");
            model.addAttribute("pets", petsForUser(currentUser));
            model.addAttribute("types", Appointment.AppointmentType.values());
            model.addAttribute("statuses", Appointment.AppointmentStatus.values());
            model.addAttribute("editMode", false);
            model.addAttribute("isStaff", isStaff(currentUser));
            return "appointments/form";
        }

        appointment.setPet(pet);
        if (appointment.getStatus() == null) {
            appointment.setStatus(Appointment.AppointmentStatus.SCHEDULED);
        }
        appointmentService.save(appointment);
        redirectAttributes.addFlashAttribute("success", "Đã đặt lịch thành công");
        return "redirect:/appointments";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id,
                          @AuthenticationPrincipal UserDetailsImpl userDetails,
                          RedirectAttributes redirectAttributes,
                          Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        Appointment apt = appointmentService.findById(id).orElse(null);
        if (apt == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy lịch hẹn");
            return "redirect:/appointments";
        }
        if (!canModifyAppointment(apt, userDetails.getUser())) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền sửa lịch hẹn này");
            return "redirect:/appointments";
        }
        model.addAttribute("appointment", apt);
        model.addAttribute("pets", petsForUser(userDetails.getUser()));
        model.addAttribute("types", Appointment.AppointmentType.values());
        model.addAttribute("statuses", Appointment.AppointmentStatus.values());
        model.addAttribute("editMode", true);
        model.addAttribute("isStaff", isStaff(userDetails.getUser()));
        return "appointments/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                        @ModelAttribute Appointment formAppointment,
                        @RequestParam Long petId,
                        @RequestParam String date,
                        @RequestParam String time,
                        @RequestParam(required = false) String status,
                        @AuthenticationPrincipal UserDetailsImpl userDetails,
                        RedirectAttributes redirectAttributes,
                        Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        Appointment existing = appointmentService.findById(id).orElse(null);
        if (existing == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy lịch hẹn");
            return "redirect:/appointments";
        }
        if (!canModifyAppointment(existing, userDetails.getUser())) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền sửa lịch hẹn này");
            return "redirect:/appointments";
        }

        Pet pet = petService.findById(petId).orElseThrow(() -> new RuntimeException("Không tìm thấy thú cưng"));
        User currentUser = userDetails.getUser();
        if (!isStaff(currentUser) && !pet.getOwner().getId().equals(currentUser.getId())) {
            redirectAttributes.addFlashAttribute("error", "Bạn không thể gán thú cưng này cho lịch hẹn");
            return "redirect:/appointments";
        }

        try {
            LocalDate d = LocalDate.parse(date);
            LocalTime t = LocalTime.parse(time);
            existing.setAppointmentDate(LocalDateTime.of(d, t));
        } catch (Exception e) {
            model.addAttribute("error", "Định dạng ngày/giờ không hợp lệ");
            model.addAttribute("appointment", existing);
            model.addAttribute("pets", petsForUser(currentUser));
            model.addAttribute("types", Appointment.AppointmentType.values());
            model.addAttribute("statuses", Appointment.AppointmentStatus.values());
            model.addAttribute("editMode", true);
            model.addAttribute("isStaff", isStaff(currentUser));
            return "appointments/form";
        }

        existing.setPet(pet);
        existing.setType(formAppointment.getType());
        existing.setReason(formAppointment.getReason());
        existing.setNotes(formAppointment.getNotes());
        if (isStaff(currentUser) && status != null && !status.isBlank()) {
            try {
                existing.setStatus(Appointment.AppointmentStatus.valueOf(status));
            } catch (IllegalArgumentException ignored) {
                // giữ nguyên trạng thái
            }
        }

        appointmentService.save(existing);
        redirectAttributes.addFlashAttribute("success", "Đã cập nhật lịch hẹn");
        return "redirect:/appointments";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                        @AuthenticationPrincipal UserDetailsImpl userDetails,
                        RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        Appointment apt = appointmentService.findById(id).orElse(null);
        if (apt == null) {
            redirectAttributes.addFlashAttribute("error", "Không tìm thấy lịch hẹn");
            return "redirect:/appointments";
        }
        if (!canModifyAppointment(apt, userDetails.getUser())) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền xóa lịch hẹn này");
            return "redirect:/appointments";
        }
        appointmentService.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Đã xóa lịch hẹn");
        return "redirect:/appointments";
    }
}
