package com.db7.j2ee_quanlythucung.controller;

import com.db7.j2ee_quanlythucung.entity.PasswordResetToken;
import com.db7.j2ee_quanlythucung.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;

@Controller
@RequiredArgsConstructor
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    @GetMapping("/forgot-password")
    public String forgotPasswordPage() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam("email") String email,
                                       RedirectAttributes redirectAttributes) {
        boolean sent = passwordResetService.createTokenAndSendEmail(email);

        if (sent) {
            redirectAttributes.addFlashAttribute("success",
                    "Đã gửi liên kết đặt lại mật khẩu đến email của bạn. Vui lòng kiểm tra hộp thư (bao gồm thư rác).");
        } else {
            redirectAttributes.addFlashAttribute("error",
                    "Không thể gửi email. Vui lòng kiểm tra cấu hình SMTP trong application.properties.");
        }
        return "redirect:/forgot-password";
    }

    @GetMapping("/reset-password")
    public String resetPasswordPage(@RequestParam("token") String token,
                                   Model model,
                                   RedirectAttributes redirectAttributes) {
        Optional<PasswordResetToken> tokenOpt = passwordResetService.getToken(token);

        if (tokenOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.");
            return "redirect:/login";
        }

        PasswordResetToken resetToken = tokenOpt.get();
        if (resetToken.getUsed() || resetToken.isExpired()) {
            redirectAttributes.addFlashAttribute("error", "Liên kết đặt lại mật khẩu đã được sử dụng hoặc hết hạn.");
            return "redirect:/login";
        }

        model.addAttribute("token", token);
        return "auth/reset-password";
    }

    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam("token") String token,
                                      @RequestParam("password") String password,
                                      @RequestParam("confirmPassword") String confirmPassword,
                                      RedirectAttributes redirectAttributes) {
        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu xác nhận không khớp.");
            return "redirect:/reset-password?token=" + token;
        }

        if (password.length() < 6) {
            redirectAttributes.addFlashAttribute("error", "Mật khẩu phải có ít nhất 6 ký tự.");
            return "redirect:/reset-password?token=" + token;
        }

        boolean success = passwordResetService.resetPassword(token, password);

        if (success) {
            redirectAttributes.addFlashAttribute("success", "Đặt lại mật khẩu thành công! Vui lòng đăng nhập.");
            return "redirect:/login";
        } else {
            redirectAttributes.addFlashAttribute("error", "Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.");
            return "redirect:/login";
        }
    }
}
