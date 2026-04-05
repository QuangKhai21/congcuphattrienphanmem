package com.db7.j2ee_quanlythucung.controller;

import com.db7.j2ee_quanlythucung.entity.LostPetReport;
import com.db7.j2ee_quanlythucung.entity.LostPetReport.ReportStatus;
import com.db7.j2ee_quanlythucung.entity.Pet;
import com.db7.j2ee_quanlythucung.entity.User;
import com.db7.j2ee_quanlythucung.security.UserDetailsImpl;
import com.db7.j2ee_quanlythucung.service.LostPetService;
import com.db7.j2ee_quanlythucung.service.PetService;
import com.db7.j2ee_quanlythucung.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/lost-pets")
@RequiredArgsConstructor
public class LostPetController {

    private final LostPetService lostPetService;
    private final PetService petService;
    private final UserRepository userRepository;

    @GetMapping
    public String list(@RequestParam(defaultValue = "0") int page,
                      @RequestParam(required = false) ReportStatus status,
                      Model model) {
        Page<LostPetReport> reports;
        if (status != null) {
            reports = lostPetService.findByStatus(status, PageRequest.of(page, 12));
        } else {
            reports = lostPetService.findAll(PageRequest.of(page, 12));
        }

        model.addAttribute("reports", reports.getContent());
        model.addAttribute("totalPages", reports.getTotalPages());
        model.addAttribute("currentPage", page);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("activeCount", lostPetService.countActiveReports());
        return "lost-pets/list";
    }

    @GetMapping("/map")
    public String map(Model model) {
        List<LostPetReport> activeReports = lostPetService.findActiveReports();
        model.addAttribute("reports", activeReports);
        model.addAttribute("centerLat", null);
        model.addAttribute("centerLng", null);
        model.addAttribute("radius", 10);
        return "lost-pets/map";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        LostPetReport report = lostPetService.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy báo cáo"));
        model.addAttribute("report", report);
        return "lost-pets/detail";
    }

    @GetMapping("/report/{id}")
    public String detailReport(@PathVariable Long id, Model model) {
        return detail(id, model);
    }

    @GetMapping("/new")
    public String createForm(@AuthenticationPrincipal UserDetailsImpl userDetails, Model model) {
        List<Pet> pets = petService.findByOwnerId(userDetails.getUser().getId(), PageRequest.of(0, 100)).getContent();
        model.addAttribute("report", new LostPetReport());
        model.addAttribute("pets", pets);
        return "lost-pets/form";
    }

    @PostMapping
    public String create(@ModelAttribute LostPetReport report,
                        @RequestParam Long petId,
                        @AuthenticationPrincipal UserDetailsImpl userDetails,
                        RedirectAttributes redirectAttributes) {
        User reporter = userRepository.findById(userDetails.getUser().getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        
        lostPetService.createReport(petId, report, reporter);
        redirectAttributes.addFlashAttribute("success", "Đã đăng báo cáo thất lạc thành công!");
        return "redirect:/lost-pets";
    }

    @PostMapping("/report/{id}/found")
    public String markAsFound(@PathVariable Long id,
                              @RequestParam(required = false) String notes,
                              @AuthenticationPrincipal UserDetailsImpl userDetails,
                              RedirectAttributes redirectAttributes) {
        LostPetReport report = lostPetService.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy báo cáo"));
        
        if (!report.getReporter().getId().equals(userDetails.getUser().getId())) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền thực hiện thao tác này");
            return "redirect:/lost-pets/report/" + id;
        }
        
        lostPetService.markAsFound(id, notes);
        redirectAttributes.addFlashAttribute("success", "Đã cập nhật trạng thái: Đã tìm thấy!");
        return "redirect:/lost-pets/report/" + id;
    }

    @PostMapping("/report/{id}/close")
    public String closeReport(@PathVariable Long id,
                             @RequestParam(required = false) String notes,
                             @AuthenticationPrincipal UserDetailsImpl userDetails,
                             RedirectAttributes redirectAttributes) {
        LostPetReport report = lostPetService.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy báo cáo"));
        
        if (!report.getReporter().getId().equals(userDetails.getUser().getId())) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền thực hiện thao tác này");
            return "redirect:/lost-pets/report/" + id;
        }
        
        lostPetService.closeReport(id, notes);
        redirectAttributes.addFlashAttribute("success", "Đã đóng báo cáo");
        return "redirect:/lost-pets/report/" + id;
    }

    @GetMapping("/my-reports")
    public String myReports(@AuthenticationPrincipal UserDetailsImpl userDetails,
                            @RequestParam(defaultValue = "0") int page,
                            Model model) {
        Page<LostPetReport> reports = lostPetService.findByReporter(
                userDetails.getUser().getId(), PageRequest.of(page, 10));
        model.addAttribute("reports", reports.getContent());
        model.addAttribute("totalPages", reports.getTotalPages());
        model.addAttribute("currentPage", page);
        return "lost-pets/my-reports";
    }

    @GetMapping("/search")
    public String search(@RequestParam String keyword, Model model) {
        List<LostPetReport> results = lostPetService.searchActiveReports(keyword);
        model.addAttribute("reports", results);
        model.addAttribute("keyword", keyword);
        model.addAttribute("isSearch", true);
        return "lost-pets/list";
    }

    @GetMapping("/nearby")
    public String nearby(@RequestParam Double lat,
                         @RequestParam Double lng,
                         @RequestParam(defaultValue = "10") Double radius,
                         Model model) {
        List<LostPetReport> reports = lostPetService.findActiveReportsInArea(lat, lng, radius);
        model.addAttribute("reports", reports);
        model.addAttribute("centerLat", lat);
        model.addAttribute("centerLng", lng);
        model.addAttribute("radius", radius);
        model.addAttribute("isNearby", true);
        return "lost-pets/map";
    }

    @GetMapping("/api/nearby")
    @ResponseBody
    public List<LostPetReport> nearbyApi(@RequestParam Double lat,
                                         @RequestParam Double lng,
                                         @RequestParam(defaultValue = "10") Double radius) {
        return lostPetService.findActiveReportsInArea(lat, lng, radius);
    }
}
