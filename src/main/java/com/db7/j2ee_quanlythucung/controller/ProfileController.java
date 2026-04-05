package com.db7.j2ee_quanlythucung.controller;

import com.db7.j2ee_quanlythucung.entity.User;
import com.db7.j2ee_quanlythucung.repository.UserRepository;
import com.db7.j2ee_quanlythucung.security.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${file.upload-dir:./uploads}")
    private String uploadDir;

    @GetMapping
    public String viewProfile(@AuthenticationPrincipal UserDetailsImpl userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        User user = userRepository.findById(userDetails.getUser().getId()).orElseThrow();
        model.addAttribute("user", user);
        return "profile/view";
    }

    @GetMapping("/edit")
    public String editForm(@AuthenticationPrincipal UserDetailsImpl userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        User user = userRepository.findById(userDetails.getUser().getId()).orElseThrow();
        model.addAttribute("user", user);
        return "profile/edit";
    }

    @PostMapping("/edit")
    public String update(@AuthenticationPrincipal UserDetailsImpl userDetails,
                         @RequestParam(required = false) String fullName,
                         @RequestParam(required = false) String email,
                         @RequestParam(required = false) String phone,
                         @RequestParam(required = false) String address,
                         @RequestParam(required = false) MultipartFile avatarFile,
                         @RequestParam(required = false) String currentPassword,
                         @RequestParam(required = false) String newPassword,
                         @RequestParam(required = false) String confirmPassword,
                         RedirectAttributes redirectAttributes,
                         Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        User user = userRepository.findById(userDetails.getUser().getId()).orElse(null);
        if (user == null) {
            return "redirect:/login";
        }

        boolean hasError = false;

        if (fullName != null && !fullName.isBlank()) {
            user.setFullName(fullName.trim());
        }
        if (email != null && !email.isBlank()) {
            if (!email.equals(user.getEmail()) && userRepository.findByEmail(email).isPresent()) {
                redirectAttributes.addFlashAttribute("error", "Email đã được sử dụng bởi tài khoản khác.");
                hasError = true;
            } else {
                user.setEmail(email.trim());
            }
        }
        if (phone != null) {
            user.setPhone(phone.trim().isEmpty() ? null : phone.trim());
        }
        if (address != null) {
            user.setAddress(address.trim().isEmpty() ? null : address.trim());
        }

        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                String uploadedUrl = saveAvatar(avatarFile, user.getId());
                user.setAvatarUrl(uploadedUrl);
            } catch (IOException e) {
                redirectAttributes.addFlashAttribute("error", "Lỗi khi tải lên ảnh đại diện: " + e.getMessage());
                hasError = true;
            }
        }

        if (newPassword != null && !newPassword.isBlank()) {
            if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPassword())) {
                redirectAttributes.addFlashAttribute("error", "Mật khẩu hiện tại không đúng.");
                hasError = true;
            } else if (!newPassword.equals(confirmPassword)) {
                redirectAttributes.addFlashAttribute("error", "Mật khẩu mới và xác nhận mật khẩu không khớp.");
                hasError = true;
            } else if (newPassword.length() < 6) {
                redirectAttributes.addFlashAttribute("error", "Mật khẩu mới phải có ít nhất 6 ký tự.");
                hasError = true;
            } else {
                user.setPassword(passwordEncoder.encode(newPassword));
            }
        }

        if (hasError) {
            model.addAttribute("user", user);
            return "profile/edit";
        }

        userRepository.save(user);
        redirectAttributes.addFlashAttribute("success", "Cập nhật thông tin thành công!");
        return "redirect:/profile";
    }

    private String saveAvatar(MultipartFile file, Long userId) throws IOException {
        Path dir = Paths.get(uploadDir, "avatars");
        Files.createDirectories(dir);

        String originalFilename = file.getOriginalFilename();
        String ext = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            ext = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String filename = "avatar_" + userId + "_" + UUID.randomUUID().toString().substring(0, 8) + ext;

        Path filePath = dir.resolve(filename);
        Files.write(filePath, file.getBytes());

        return "/uploads/avatars/" + filename;
    }
}
