package com.db7.j2ee_quanlythucung.controller;

import com.db7.j2ee_quanlythucung.security.UserDetailsImpl;
import com.db7.j2ee_quanlythucung.service.NotificationService;
import com.db7.j2ee_quanlythucung.util.AvatarUrlFormatter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.List;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalControllerAdvice {

    private final NotificationService notificationService;
    private final AvatarUrlFormatter avatarUrlFormatter;

    @ModelAttribute
    public void addAvatarUrlsHelper(Model model) {
        model.addAttribute("avatarUrls", avatarUrlFormatter);
    }

    @ModelAttribute
    public void addGlobalAttributes(Model model, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails != null && userDetails.getUser() != null) {
            Long userId = userDetails.getUser().getId();
            model.addAttribute("notifications", notificationService.getNotifications(userId, 5));
            model.addAttribute("notificationCount", notificationService.getUnreadCount(userId));
            model.addAttribute("userName", userDetails.getUser().getFullName());
        } else {
            model.addAttribute("notifications", List.of());
            model.addAttribute("notificationCount", 0);
        }
    }
}
