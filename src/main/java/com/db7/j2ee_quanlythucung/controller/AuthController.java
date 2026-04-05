package com.db7.j2ee_quanlythucung.controller;

import com.db7.j2ee_quanlythucung.entity.Role;
import com.db7.j2ee_quanlythucung.entity.User;
import com.db7.j2ee_quanlythucung.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Set;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    @GetMapping("/login")
    public String loginPage() {
        return "auth/login";
    }

    /**
     * GET /logout：仅作友好重定向（书签或误输入）；真正登出仍由 Spring Security 的 POST /logout 处理。
     */
    @GetMapping("/logout")
    public String logoutGetFallback() {
        return "redirect:/";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("user", new User());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("user") User user, BindingResult result,
                          RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "auth/register";
        }
        if (userService.existsByUsername(user.getUsername())) {
            result.rejectValue("username", "error.username", "Tên đăng nhập đã tồn tại");
            return "auth/register";
        }
        if (userService.existsByEmail(user.getEmail())) {
            result.rejectValue("email", "error.email", "Email đã được sử dụng");
            return "auth/register";
        }
        userService.create(user, "ROLE_CUSTOMER");
        redirectAttributes.addFlashAttribute("success", "Đăng ký thành công! Vui lòng đăng nhập.");
        return "redirect:/login";
    }
}
