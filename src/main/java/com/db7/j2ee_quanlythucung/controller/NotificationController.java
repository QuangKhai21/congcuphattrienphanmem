package com.db7.j2ee_quanlythucung.controller;

import com.db7.j2ee_quanlythucung.security.UserDetailsImpl;
import com.db7.j2ee_quanlythucung.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Collections;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping(value = "/api/header-data", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> headerData(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (userDetails == null || userDetails.getUser() == null) {
            out.put("count", 0);
            out.put("notifications", List.of());
            return out;
        }
        Long userId = userDetails.getUser().getId();
        List<NotificationService.NotificationItem> notifications =
                notificationService.getNotifications(userId, 5);
        int unreadCount = notificationService.getUnreadCount(userId);
        List<Map<String, Object>> items = notifications.stream().map(n -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", n.getId());
            m.put("title", n.getTitle());
            m.put("message", n.getMessage());
            m.put("link", n.getLink());
            m.put("read", n.isRead());
            m.put("timeAgo", n.getTimeAgo());
            m.put("iconClass", n.getIconClass());
            return m;
        }).collect(Collectors.toList());
        out.put("count", unreadCount);
        out.put("notifications", items);
        return out;
    }

    /** 展开铃铛下拉时标为已读（需 CSRF POST） */
    @PostMapping(value = "/api/mark-all-read", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Map<String, Object> markAllReadJson(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails != null && userDetails.getUser() != null) {
            notificationService.markAllAsReadForUser(userDetails.getUser().getId());
        }
        return Collections.singletonMap("ok", true);
    }

    @GetMapping
    public String allNotifications(Model model, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        
        List<NotificationService.NotificationItem> notifications = 
                notificationService.getNotifications(userDetails.getUser().getId(), 0);
        
        model.addAttribute("notifications", notifications);
        model.addAttribute("totalNotifications", notifications.size());
        model.addAttribute("unreadCount", notificationService.getUnreadCount(userDetails.getUser().getId()));
        
        return "notifications/list";
    }

    @GetMapping("/mark-read/{id}")
    public String markAsRead(@PathVariable String id, RedirectAttributes redirectAttributes) {
        notificationService.markAsRead(id);
        redirectAttributes.addFlashAttribute("success", "Đã đánh dấu là đã đọc");
        return "redirect:/notifications";
    }

    @GetMapping("/mark-all-read")
    public String markAllAsRead(@AuthenticationPrincipal UserDetailsImpl userDetails,
                                RedirectAttributes redirectAttributes) {
        if (userDetails != null && userDetails.getUser() != null) {
            notificationService.markAllAsReadForUser(userDetails.getUser().getId());
        }
        redirectAttributes.addFlashAttribute("success", "Đã đánh dấu tất cả là đã đọc");
        return "redirect:/notifications";
    }
}
