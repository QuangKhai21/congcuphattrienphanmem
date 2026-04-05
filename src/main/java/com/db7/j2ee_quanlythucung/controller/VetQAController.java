package com.db7.j2ee_quanlythucung.controller;

import com.db7.j2ee_quanlythucung.entity.User;
import com.db7.j2ee_quanlythucung.entity.VetQuestion;
import com.db7.j2ee_quanlythucung.entity.VetQuestion.QuestionCategory;
import com.db7.j2ee_quanlythucung.security.UserDetailsImpl;
import com.db7.j2ee_quanlythucung.service.UserService;
import com.db7.j2ee_quanlythucung.service.VetQAService;
import com.db7.j2ee_quanlythucung.service.VetService;
import com.db7.j2ee_quanlythucung.util.AvatarUrlUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/vet-qa")
@RequiredArgsConstructor
public class VetQAController {

    private final VetService vetService;
    private final VetQAService vetQAService;
    private final UserService userService;

    // ── Trang chính: danh sách bác sĩ + FAQ ────────────────────────────────
    @GetMapping
    public String list(@RequestParam(required = false) String specialization,
                       @RequestParam(required = false) String province,
                       @RequestParam(required = false) String keyword,
                       @RequestParam(defaultValue = "0") int page,
                       @AuthenticationPrincipal UserDetailsImpl userDetails,
                       Model model) {
        page = Math.max(0, page);
        Page<?> vets = vetService.search(specialization, province, null, keyword, page, 12);
        Map<Long, String> vetCardAvatarUrls = new LinkedHashMap<>();
        for (Object row : vets.getContent()) {
            if (row instanceof User u) {
                String n = AvatarUrlUtils.normalize(u.getAvatarUrl());
                if (n != null) {
                    vetCardAvatarUrls.put(u.getId(), n);
                }
            }
        }
        model.addAttribute("vetCardAvatarUrls", vetCardAvatarUrls);
        model.addAttribute("vets", vets.getContent());
        model.addAttribute("totalPages", vets.getTotalPages());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalVets", vets.getTotalElements());
        model.addAttribute("selectedSpecialization", specialization);
        model.addAttribute("selectedProvince", province);
        model.addAttribute("keyword", keyword);
        model.addAttribute("specializations", vetService.getSpecializations());
        model.addAttribute("provinces", vetService.getProvinces());
        boolean currentUserIsVet = false;
        boolean currentUserVetPending = false;
        Long currentUserId = null;
        if (userDetails != null) {
            currentUserId = userDetails.getUser().getId();
            Optional<User> opt = userService.findById(currentUserId);
            currentUserIsVet = opt.map(u -> Boolean.TRUE.equals(u.getIsVet())).orElse(false);
            currentUserVetPending = opt.map(u -> Boolean.TRUE.equals(u.getIsVetPending())).orElse(false);
        }
        model.addAttribute("currentUserId", currentUserId);
        model.addAttribute("currentUserIsVet", currentUserIsVet);
        model.addAttribute("currentUserVetPending", currentUserVetPending);
        return "vet-qa/list";
    }

    @GetMapping("/register")
    public String registerForm(@AuthenticationPrincipal UserDetailsImpl userDetails,
                               RedirectAttributes redirectAttributes,
                               Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        User me = userService.findById(userDetails.getUser().getId()).orElseThrow();
        if (Boolean.TRUE.equals(me.getIsVet())) {
            return "redirect:/vet-qa/edit";
        }
        if (Boolean.TRUE.equals(me.getIsVetPending())) {
            return "redirect:/vet-qa";
        }
        model.addAttribute("specializations", vetService.getSpecializations());
        model.addAttribute("provinces", vetService.getProvinces());
        return "vet-qa/register";
    }

    @GetMapping("/edit")
    public String editForm(@AuthenticationPrincipal UserDetailsImpl userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        User me = userService.findById(userDetails.getUser().getId()).orElseThrow();
        if (!Boolean.TRUE.equals(me.getIsVet())) {
            return "redirect:/vet-qa/register";
        }
        model.addAttribute("editing", true);
        model.addAttribute("specializations", vetService.getSpecializations());
        model.addAttribute("provinces", vetService.getProvinces());
        model.addAttribute("currentVetProfile", me);
        return "vet-qa/register";
    }

    @PostMapping("/edit")
    public String editSubmit(@AuthenticationPrincipal UserDetailsImpl userDetails,
                             @RequestParam String clinicName,
                             @RequestParam String specialization,
                             @RequestParam String province,
                             @RequestParam(required = false) String city,
                             @RequestParam(required = false) String clinicAddress,
                             @RequestParam(required = false) String bio,
                             @RequestParam(required = false) String licenseNumber,
                             @RequestParam(required = false) Integer yearsExperience,
                             RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        if (clinicName == null || clinicName.isBlank()
                || specialization == null || specialization.isBlank()
                || province == null || province.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng điền tên phòng khám, chuyên ngành và tỉnh/thành.");
            return "redirect:/vet-qa/edit";
        }
        try {
            userService.updateVetProfile(
                    userDetails.getUser().getId(),
                    clinicName,
                    specialization,
                    province,
                    city,
                    clinicAddress,
                    bio,
                    licenseNumber,
                    yearsExperience);
            redirectAttributes.addFlashAttribute("success", "Cập nhật hồ sơ bác sĩ thành công!");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/vet-qa/edit";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không thể cập nhật: " + e.getMessage());
            return "redirect:/vet-qa/edit";
        }
        return "redirect:/vet-qa";
    }

    @PostMapping("/register")
    public String registerSubmit(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                 @RequestParam String clinicName,
                                 @RequestParam String specialization,
                                 @RequestParam String province,
                                 @RequestParam(required = false) String city,
                                 @RequestParam(required = false) String clinicAddress,
                                 @RequestParam(required = false) String bio,
                                 @RequestParam(required = false) String licenseNumber,
                                 @RequestParam(required = false) Integer yearsExperience,
                                 RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        if (clinicName == null || clinicName.isBlank()
                || specialization == null || specialization.isBlank()
                || province == null || province.isBlank()) {
            redirectAttributes.addFlashAttribute("error", "Vui lòng điền tên phòng khám, chuyên ngành và tỉnh/thành.");
            return "redirect:/vet-qa/register";
        }
        try {
            userService.registerAsVet(
                    userDetails.getUser().getId(),
                    clinicName,
                    specialization,
                    province,
                    city,
                    clinicAddress,
                    bio,
                    licenseNumber,
                    yearsExperience);
            redirectAttributes.addFlashAttribute("success",
                    "Đã gửi yêu cầu đăng ký bác sĩ. Quản trị viên sẽ xem xét; sau khi duyệt, hồ sơ của bạn mới xuất hiện trên trang bác sĩ.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/vet-qa/register";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không thể đăng ký: " + e.getMessage());
            return "redirect:/vet-qa/register";
        }
        return "redirect:/vet-qa";
    }

    @PostMapping("/unregister")
    public String unregister(@AuthenticationPrincipal UserDetailsImpl userDetails,
                             RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        try {
            userService.unregisterAsVet(userDetails.getUser().getId());
            redirectAttributes.addFlashAttribute("success", "Đã hủy đăng ký bác sĩ. Bạn không còn hiển thị trong danh sách bác sĩ.");
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không thể hủy đăng ký: " + e.getMessage());
        }
        return "redirect:/vet-qa";
    }

    @PostMapping("/cancel-pending")
    public String cancelPending(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        try {
            userService.cancelPendingVetRequest(userDetails.getUser().getId());
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Không thể hủy yêu cầu: " + e.getMessage());
        }
        return "redirect:/vet-qa";
    }

    // ── FAQ phổ biến (hiển thị inline, không chuyển trang) ──────────────────
    @GetMapping("/faq")
    public String faq(Model model) {
        model.addAttribute("categories", QuestionCategory.values());
        return "vet-qa/faq :: faq-fragment";
    }

    /** Đường dẫn cũ /vet-qa/ask — chức năng đặt câu hỏi đã gỡ, chuyển về danh sách bác sĩ. */
    @RequestMapping(value = "/ask", method = {RequestMethod.GET, RequestMethod.POST})
    public String askDeprecated() {
        return "redirect:/vet-qa";
    }

    // ── Xem chi tiết câu hỏi ────────────────────────────────────────────────
    @GetMapping("/question/{id}")
    public String detail(@PathVariable Long id, Model model) {
        VetQuestion question = vetQAService.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy câu hỏi"));
        vetQAService.incrementViewCount(id);
        question = vetQAService.findById(id).orElse(question);
        model.addAttribute("question", question);
        return "vet-qa/detail";
    }

    @PostMapping("/question/{id}/answer")
    @PreAuthorize("hasAnyRole('ADMIN', 'VET')")
    public String answer(@PathVariable Long id,
                         @RequestParam String content,
                         @AuthenticationPrincipal UserDetailsImpl userDetails,
                         RedirectAttributes redirectAttributes) {
        try {
            vetQAService.answerQuestion(id, content, userDetails.getUser().getId());
            redirectAttributes.addFlashAttribute("success", "Đã trả lời câu hỏi!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/vet-qa/question/" + id;
    }

    @PostMapping("/question/{id}/like")
    @ResponseBody
    public void like(@PathVariable Long id) {
        vetQAService.likeQuestion(id);
    }

    @PostMapping("/question/{id}/close")
    public String close(@PathVariable Long id,
                        @AuthenticationPrincipal UserDetailsImpl userDetails,
                        RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        VetQuestion question = vetQAService.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy câu hỏi"));
        if (!question.getAuthor().getId().equals(userDetails.getUser().getId())) {
            redirectAttributes.addFlashAttribute("error", "Bạn không có quyền đóng câu hỏi này");
            return "redirect:/vet-qa/question/" + id;
        }
        vetQAService.closeQuestion(id);
        redirectAttributes.addFlashAttribute("success", "Đã đóng câu hỏi");
        return "redirect:/vet-qa/question/" + id;
    }

    @GetMapping("/my-questions")
    public String myQuestions(@AuthenticationPrincipal UserDetailsImpl userDetails,
                              @RequestParam(defaultValue = "0") int page,
                              Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        page = Math.max(0, page);
        Page<VetQuestion> questions = vetQAService.findByAuthor(userDetails.getUser().getId(), page, 10);
        model.addAttribute("questions", questions.getContent());
        model.addAttribute("totalPages", questions.getTotalPages());
        model.addAttribute("currentPage", page);
        return "vet-qa/my-questions";
    }
}






