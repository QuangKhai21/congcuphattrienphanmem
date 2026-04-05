package com.db7.j2ee_quanlythucung.controller;

import com.db7.j2ee_quanlythucung.entity.Breed;
import com.db7.j2ee_quanlythucung.entity.Notification.NotificationType;
import com.db7.j2ee_quanlythucung.entity.PetCategory;
import com.db7.j2ee_quanlythucung.entity.Role;
import com.db7.j2ee_quanlythucung.entity.User;
import com.db7.j2ee_quanlythucung.repository.RoleRepository;
import com.db7.j2ee_quanlythucung.repository.UserRepository;
import com.db7.j2ee_quanlythucung.security.UserDetailsImpl;
import com.db7.j2ee_quanlythucung.service.BreedService;
import com.db7.j2ee_quanlythucung.service.NotificationService;
import com.db7.j2ee_quanlythucung.service.PetCategoryService;
import com.db7.j2ee_quanlythucung.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'MANAGER')")
public class AdminController {

    private final PetCategoryService petCategoryService;
    private final BreedService breedService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    // ==================== Dashboard ====================

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("totalUsers", userRepository.countAllUsers());
        model.addAttribute("activeUsers", userRepository.countActiveUsers());
        model.addAttribute("officialVetsCount", userRepository.countByIsVetTrue());
        model.addAttribute("pendingVetsCount", userRepository.countByIsVetPendingTrue());
        return "admin/dashboard";
    }

    // ==================== Người dùng (Users) ====================

    @GetMapping("/users")
    public String listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {
        Page<User> userPage = userService.search(keyword, PageRequest.of(page, size, Sort.by("id").descending()));
        model.addAttribute("users", userPage.getContent());
        model.addAttribute("keyword", keyword);
        model.addAttribute("currentPage", userPage.getNumber());
        model.addAttribute("totalPages", userPage.getTotalPages());
        return "admin/users/list";
    }

    @GetMapping("/users/new")
    public String newUserForm(Model model) {
        model.addAttribute("user", new User());
        model.addAttribute("roleTypes", Role.RoleType.values());
        model.addAttribute("selectedRoleNames", List.of());
        return "admin/users/form";
    }

    @GetMapping("/users/{id}/edit")
    public String editUserForm(@PathVariable Long id, Model model) {
        User user = userService.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
        List<String> selectedRoleNames = user.getRoles().stream()
                .map(r -> r.getName().name())
                .collect(Collectors.toList());
        model.addAttribute("user", user);
        model.addAttribute("roleTypes", Role.RoleType.values());
        model.addAttribute("selectedRoleNames", selectedRoleNames);
        return "admin/users/form";
    }

    @PostMapping("/users")
    public String createUser(
            @Valid @ModelAttribute User user,
            @RequestParam(required = false) String newPassword,
            @RequestParam(value = "selectedRoles", required = false) List<String> selectedRoles,
            BindingResult result,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Dữ liệu không hợp lệ");
            return "redirect:/admin/users/new";
        }
        try {
            Set<Role> roles = new HashSet<>();
            if (selectedRoles != null) {
                for (String roleName : selectedRoles) {
                    roleRepository.findByName(Role.RoleType.valueOf(roleName))
                            .ifPresent(roles::add);
                }
            }
            user.setRoles(roles);
            if (newPassword != null && !newPassword.isBlank()) {
                user.setPassword(passwordEncoder.encode(newPassword));
            } else {
                user.setPassword(passwordEncoder.encode("123456"));
            }
            userRepository.save(user);
            redirectAttributes.addFlashAttribute("success", "Thêm người dùng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/admin/users/new";
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}")
    public String updateUser(
            @PathVariable Long id,
            @Valid @ModelAttribute User user,
            @RequestParam(required = false) String newPassword,
            @RequestParam(value = "selectedRoles", required = false) List<String> selectedRoles,
            BindingResult result,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "Dữ liệu không hợp lệ");
            return "redirect:/admin/users/" + id + "/edit";
        }
        try {
            User existing = userService.findById(id)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người dùng"));
            existing.setFullName(user.getFullName());
            existing.setEmail(user.getEmail());
            existing.setPhone(user.getPhone());
            existing.setAddress(user.getAddress());
            existing.setAvatarUrl(user.getAvatarUrl());
            existing.setEnabled(user.getEnabled());
            if (newPassword != null && !newPassword.isBlank()) {
                existing.setPassword(passwordEncoder.encode(newPassword));
            }
            Set<Role> roles = new HashSet<>();
            if (selectedRoles != null) {
                for (String roleName : selectedRoles) {
                    roleRepository.findByName(Role.RoleType.valueOf(roleName))
                            .ifPresent(roles::add);
                }
            }
            existing.setRoles(roles);
            userRepository.save(existing);
            redirectAttributes.addFlashAttribute("success", "Cập nhật người dùng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
            return "redirect:/admin/users/" + id + "/edit";
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/reset-password")
    public String resetPassword(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.resetPassword(id);
            redirectAttributes.addFlashAttribute("success", "Đặt lại mật khẩu thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/toggle-status")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.toggleStatus(id);
            redirectAttributes.addFlashAttribute("success", "Thay đổi trạng thái thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    @PostMapping("/users/{id}/delete")
    public String deleteUser(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            userService.delete(id);
            redirectAttributes.addFlashAttribute("success", "Xóa người dùng thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi xóa: " + e.getMessage());
        }
        return "redirect:/admin/users";
    }

    // ==================== Loài (PetCategory) ====================

    @GetMapping("/categories")
    public String listCategories(Model model) {
        List<PetCategory> categories = petCategoryService.findAll();
        model.addAttribute("categories", categories);
        return "admin/categories";
    }

    @GetMapping("/categories/new")
    public String newCategoryForm(Model model) {
        model.addAttribute("category", new PetCategory());
        return "admin/category-form";
    }

    @GetMapping("/categories/{id}/edit")
    public String editCategoryForm(@PathVariable Long id, Model model) {
        PetCategory category = petCategoryService.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy loài"));
        model.addAttribute("category", category);
        return "admin/category-form";
    }

    @PostMapping("/categories/save")
    public String saveCategory(@Valid @ModelAttribute PetCategory category,
                               @RequestParam(required = false) Long id,
                               RedirectAttributes redirectAttributes) {
        try {
            if (id != null) {
                category.setId(id);
            }
            petCategoryService.save(category);
            redirectAttributes.addFlashAttribute("success", "Lưu loài thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/{id}/delete")
    public String deleteCategory(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            petCategoryService.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Xóa loài thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi xóa: " + e.getMessage());
        }
        return "redirect:/admin/categories";
    }

    // ==================== Giống (Breed) ====================

    @GetMapping("/breeds")
    public String listBreeds(Model model,
                            @RequestParam(required = false) Long categoryId) {
        List<Breed> breeds;
        if (categoryId != null) {
            breeds = breedService.findByCategoryId(categoryId);
            model.addAttribute("selectedCategoryId", categoryId);
        } else {
            breeds = breedService.findAll();
        }
        List<PetCategory> categories = petCategoryService.findAll();
        model.addAttribute("breeds", breeds);
        model.addAttribute("categories", categories);
        return "admin/breeds";
    }

    @GetMapping("/breeds/new")
    public String newBreedForm(Model model) {
        List<PetCategory> categories = petCategoryService.findAll();
        model.addAttribute("breed", new Breed());
        model.addAttribute("categories", categories);
        return "admin/breed-form";
    }

    @GetMapping("/breeds/{id}/edit")
    public String editBreedForm(@PathVariable Long id, Model model) {
        Breed breed = breedService.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy giống"));
        List<PetCategory> categories = petCategoryService.findAll();
        model.addAttribute("breed", breed);
        model.addAttribute("categories", categories);
        return "admin/breed-form";
    }

    @PostMapping("/breeds/save")
    public String saveBreed(@Valid @ModelAttribute Breed breed,
                            @RequestParam(required = false) Long id,
                            @RequestParam Long categoryId,
                            RedirectAttributes redirectAttributes) {
        try {
            if (id != null) {
                breed.setId(id);
            }
            breedService.save(breed, categoryId);
            redirectAttributes.addFlashAttribute("success", "Lưu giống thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/breeds";
    }

    @PostMapping("/breeds/{id}/delete")
    public String deleteBreed(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            breedService.deleteById(id);
            redirectAttributes.addFlashAttribute("success", "Xóa giống thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi khi xóa: " + e.getMessage());
        }
        return "redirect:/admin/breeds";
    }

    // ==================== Bác sĩ (Doctors) ====================

    @GetMapping("/doctors")
    public String listDoctors(Model model) {
        List<User> pendingRequests = userService.findPendingVetRequests();
        List<User> approvedVets = userService.findApprovedVets();
        List<User> blockedVets = userService.findBlockedVets();
        List<User> removedVets = userService.findRemovedVets();
        model.addAttribute("pendingRequests", pendingRequests);
        model.addAttribute("approvedVets", approvedVets);
        model.addAttribute("blockedVets", blockedVets);
        model.addAttribute("removedVets", removedVets);
        return "admin/doctors/list";
    }

    @PostMapping("/doctors/approve")
    public String approveVetRequest(@RequestParam Long userId, RedirectAttributes redirectAttributes) {
        try {
            userService.approveVetRequest(userId);
            var opt = userService.findById(userId);
            opt.ifPresent(u -> notificationService.sendAdminNotification(
                    userId,
                    "Đăng ký bác sĩ được duyệt!",
                    "Yêu cầu đăng ký bác sĩ của bạn đã được quản trị viên duyệt. Hồ sơ bác sĩ của bạn hiện hiển thị công khai.",
                    NotificationType.VET_APPROVED,
                    "/vet-qa/edit"));
            redirectAttributes.addFlashAttribute("success", "Duyệt bác sĩ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/doctors";
    }

    @PostMapping("/doctors/reject")
    public String rejectVetRequest(@RequestParam Long userId,
                                   @RequestParam(required = false) String note,
                                   RedirectAttributes redirectAttributes) {
        try {
            userService.rejectVetRequest(userId, note);
            notificationService.sendAdminNotification(
                    userId,
                    "Yêu cầu đăng ký bác sĩ bị từ chối",
                    note != null && !note.isBlank()
                            ? "Yêu cầu đăng ký bác sĩ đã bị từ chối. Lý do: " + note
                            : "Yêu cầu đăng ký bác sĩ của bạn đã bị từ chối bởi quản trị viên.",
                    NotificationType.VET_REJECTED,
                    null);
            redirectAttributes.addFlashAttribute("success", "Đã từ chối yêu cầu đăng ký bác sĩ!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/doctors";
    }

    @PostMapping("/doctors/add")
    public String addVetByAdmin(@RequestParam String usernameOrEmail,
                                @RequestParam(required = false) String returnTo,
                                RedirectAttributes redirectAttributes) {
        try {
            User vet = userService.addVetByAdmin(usernameOrEmail);
            notificationService.sendAdminNotification(
                    vet.getId(),
                    "Bạn đã được thêm làm bác sĩ!",
                    "Quản trị viên đã thêm bạn làm bác sĩ. Hồ sơ bác sĩ của bạn hiện hiển thị công khai.",
                    NotificationType.VET_APPROVED,
                    "/vet-qa/edit");
            redirectAttributes.addFlashAttribute("success", "Thêm bác sĩ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        if ("dashboard".equals(returnTo)) {
            return "redirect:/admin";
        }
        return "redirect:/admin/doctors";
    }

    @PostMapping("/doctors/remove")
    public String removeVet(@RequestParam Long userId, RedirectAttributes redirectAttributes) {
        try {
            userService.removeVet(userId);
            notificationService.sendAdminNotification(
                    userId,
                    "Tài khoản bác sĩ đã bị hủy",
                    "Quản trị viên đã hủy quyền bác sĩ của bạn. Bạn có thể liên hệ quản trị viên để được xem xét lại.",
                    NotificationType.VET_REMOVED,
                    "/vet-qa");
            redirectAttributes.addFlashAttribute("success", "Đã hủy bác sĩ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/doctors";
    }

    @PostMapping("/doctors/block")
    public String blockVetRegistration(@RequestParam Long userId, RedirectAttributes redirectAttributes) {
        try {
            userService.blockVetRegistration(userId);
            notificationService.sendAdminNotification(
                    userId,
                    "Tài khoản bị chặn đăng ký bác sĩ",
                    "Tài khoản của bạn đã bị chặn không được đăng ký làm bác sĩ. Vui lòng liên hệ quản trị viên nếu cần.",
                    NotificationType.VET_BLOCKED,
                    null);
            redirectAttributes.addFlashAttribute("success", "Đã chặn tài khoản không được đăng ký làm bác sĩ!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/doctors";
    }

    @PostMapping("/doctors/unblock")
    public String unblockVetRegistration(@RequestParam Long userId, RedirectAttributes redirectAttributes) {
        try {
            userService.unblockVetRegistration(userId);
            notificationService.sendAdminNotification(
                    userId,
                    "Đã mở chặn đăng ký bác sĩ",
                    "Tài khoản của bạn đã được mở chặn đăng ký bác sĩ. Bạn có thể đăng ký lại.",
                    NotificationType.VET_REGISTRATION_UNBLOCKED,
                    "/vet-qa/register");
            redirectAttributes.addFlashAttribute("success", "Đã mở chặn đăng ký bác sĩ cho tài khoản!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/doctors";
    }

    @PostMapping("/doctors/restore")
    public String restoreVet(@RequestParam Long userId, RedirectAttributes redirectAttributes) {
        try {
            userService.restoreVet(userId);
            notificationService.sendAdminNotification(
                    userId,
                    "Bác sĩ đã được thêm lại!",
                    "Quản trị viên đã khôi phục tài khoản bác sĩ của bạn. Hồ sơ bác sĩ của bạn hiện hiển thị công khai.",
                    NotificationType.VET_RESTORED,
                    "/vet-qa/edit");
            redirectAttributes.addFlashAttribute("success", "Đã thêm lại bác sĩ thành công!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Lỗi: " + e.getMessage());
        }
        return "redirect:/admin/doctors";
    }
}
