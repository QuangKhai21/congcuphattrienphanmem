package com.db7.j2ee_quanlythucung.service;

import com.db7.j2ee_quanlythucung.entity.Appointment;
import com.db7.j2ee_quanlythucung.entity.Notification;
import com.db7.j2ee_quanlythucung.entity.Notification.NotificationType;
import com.db7.j2ee_quanlythucung.entity.User;
import com.db7.j2ee_quanlythucung.entity.Vaccination;
import com.db7.j2ee_quanlythucung.repository.AppointmentRepository;
import com.db7.j2ee_quanlythucung.repository.NotificationRepository;
import com.db7.j2ee_quanlythucung.repository.UserRepository;
import com.db7.j2ee_quanlythucung.repository.VaccinationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final VaccinationRepository vaccinationRepository;
    private final AppointmentRepository appointmentRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    // ── Gửi thông báo từ admin ────────────────────────────────────────────

    @Transactional
    public void sendAdminNotification(Long userId, String title, String message, NotificationType type, String link) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return;
        Notification notif = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type != null ? type : NotificationType.ADMIN)
                .link(link)
                .isRead(false)
                .build();
        notificationRepository.save(notif);
    }

    @Transactional
    public void sendAdminNotification(Long userId, String title, String message, String link) {
        sendAdminNotification(userId, title, message, NotificationType.ADMIN, link);
    }

    // ── Đọc thông báo thực (admin) ────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public List<Notification> getUnreadUserNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
    }

    @Transactional(readOnly = true)
    public int countUnread(Long userId) {
        return (int) notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public void markRead(Long notificationId) {
        notificationRepository.findById(notificationId).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepository.save(n);
        });
    }

    @Transactional
    public void markAllRead(Long userId) {
        notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .forEach(n -> {
                    n.setIsRead(true);
                    notificationRepository.save(n);
                });
    }

    @Transactional
    public void markAllRead() {
        notificationRepository.findAll().forEach(n -> {
            if (!Boolean.TRUE.equals(n.getIsRead())) {
                n.setIsRead(true);
                notificationRepository.save(n);
            }
        });
    }

    // ── Ghép thông báo thực + ảo (dùng cho bell + header) ────────────────

    @Transactional(readOnly = true)
    public List<NotificationItem> getNotifications(Long userId, int limit) {
        List<NotificationItem> notifications = new ArrayList<>();

        // 1. Thông báo thực từ admin
        List<Notification> real = (userId != null)
                ? notificationRepository.findLatestByUserId(userId)
                : List.of();
        for (Notification n : real) {
            notifications.add(toItem(n));
        }

        // 2. Thông báo ảo: lịch tiêm sắp tới
        LocalDate today = LocalDate.now();
        List<Vaccination> vaccinations = (userId != null)
                ? vaccinationRepository.findByNextDueDateBetweenAndOwnerWithPetAndOwner(today, today.plusDays(7), userId)
                : vaccinationRepository.findByNextDueDateBetweenWithPetAndOwner(today, today.plusDays(7));
        for (Vaccination v : vaccinations) {
            long daysUntil = ChronoUnit.DAYS.between(today, v.getNextDueDate());
            String msg;
            if (daysUntil < 0) {
                msg = "Đã quá hạn tiêm " + v.getVaccineName() + " (" + Math.abs(daysUntil) + " ngày)";
            } else if (daysUntil == 0) {
                msg = "Hôm nay là ngày tiêm " + v.getVaccineName();
            } else {
                msg = "Còn " + daysUntil + " ngày đến lịch tiêm " + v.getVaccineName();
            }
            notifications.add(NotificationItem.builder()
                    .id("vac-" + v.getId())
                    .title(v.getVaccineName())
                    .message(msg)
                    .type("vaccination")
                    .link("/pets/" + v.getPet().getId() + "/vaccinations")
                    .iconClass("bi bi-shield-check text-success")
                    .read(v.getReminderSent() != null && v.getReminderSent())
                    .timeAgo(formatTimeAgo(v.getNextDueDate().atStartOfDay()))
                    .createdAt(v.getCreatedAt())
                    .build());
        }

        // 3. Thông báo ảo: lịch hẹn hôm nay
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);
        List<Appointment> todayAppts = (userId != null)
                ? appointmentRepository.findByDateRangeAndOwnerWithPetAndOwner(startOfDay, endOfDay, userId)
                : appointmentRepository.findByDateRangeWithPetAndOwner(startOfDay, endOfDay);
        for (Appointment a : todayAppts) {
            String time = a.getAppointmentDate().format(DateTimeFormatter.ofPattern("HH:mm"));
            notifications.add(NotificationItem.builder()
                    .id("apt-" + a.getId())
                    .title("Lịch hẹn hôm nay")
                    .message(a.getPet().getName() + " - " + a.getType() + " lúc " + time)
                    .type("appointment")
                    .link("/appointments")
                    .iconClass("bi bi-calendar-event text-warning")
                    .read(a.isHeaderNotificationSeen())
                    .timeAgo("Hôm nay")
                    .createdAt(a.getCreatedAt())
                    .build());
        }

        // Sort: admin thực theo createdAt desc, ảo theo ngày
        notifications.sort((a, b) -> {
            if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
            if (a.getCreatedAt() == null) return 1;
            if (b.getCreatedAt() == null) return -1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });

        if (limit > 0 && notifications.size() > limit) {
            return notifications.subList(0, limit);
        }
        return notifications;
    }

    @Transactional(readOnly = true)
    public int getUnreadCount(Long userId) {
        int realUnread = countUnread(userId);
        List<NotificationItem> virtual = getNotifications(userId, 0);
        int virtualUnread = (int) virtual.stream().filter(n -> !n.isRead()).count();
        return realUnread + virtualUnread;
    }

    @Transactional
    public void markAsRead(String notificationId) {
        if (notificationId == null || notificationId.isBlank()) return;
        // Thông báo thực (số thuần)
        if (notificationId.matches("\\d+")) {
            markRead(Long.parseLong(notificationId));
            return;
        }
        // Thông báo ảo vac-123
        try {
            if (notificationId.startsWith("vac-")) {
                Long id = Long.parseLong(notificationId.substring(4));
                vaccinationRepository.findById(id).ifPresent(v -> {
                    v.setReminderSent(true);
                    vaccinationRepository.save(v);
                });
            } else if (notificationId.startsWith("apt-")) {
                Long id = Long.parseLong(notificationId.substring(4));
                appointmentRepository.findById(id).ifPresent(a -> {
                    a.setHeaderNotificationSeen(true);
                    appointmentRepository.save(a);
                });
            }
        } catch (NumberFormatException ignored) {}
    }

    @Transactional
    public void markAllAsReadForUser(Long userId) {
        if (userId == null) return;
        notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .forEach(n -> {
                    n.setIsRead(true);
                    notificationRepository.save(n);
                });
        for (NotificationItem n : getNotifications(userId, 0)) {
            if (!n.isRead()) markAsRead(n.getId());
        }
    }

    // ── Chuyển entity → DTO ────────────────────────────────────────────────

    private NotificationItem toItem(Notification n) {
        String iconClass = switch (n.getType()) {
            case VET_APPROVED -> "bi bi-check-circle-fill text-success";
            case VET_REJECTED, VET_BLOCKED -> "bi bi-x-circle-fill text-danger";
            case VET_REMOVED -> "bi bi-trash-fill text-secondary";
            case VET_RESTORED, VET_REGISTRATION_UNBLOCKED -> "bi bi-arrow-counterclockwise text-primary";
            default -> "bi bi-bell-fill text-warning";
        };
        return NotificationItem.builder()
                .id(String.valueOf(n.getId()))
                .title(n.getTitle())
                .message(n.getMessage())
                .type(n.getType().name().toLowerCase())
                .link(n.getLink())
                .iconClass(iconClass)
                .read(Boolean.TRUE.equals(n.getIsRead()))
                .timeAgo(formatTimeAgo(n.getCreatedAt()))
                .createdAt(n.getCreatedAt())
                .build();
    }

    private String formatTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) return "";
        long days = ChronoUnit.DAYS.between(dateTime.toLocalDate(), LocalDate.now());
        if (days == 0) return "Hôm nay";
        if (days == 1) return "Hôm qua";
        if (days < 7) return days + " ngày trước";
        if (days < 30) return (days / 7) + " tuần trước";
        return dateTime.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
    }

    // ── DTO ─────────────────────────────────────────────────────────────────

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class NotificationItem {
        private String id;
        private String title;
        private String message;
        private String type;
        private String link;
        private String iconClass;
        private boolean read;
        private String timeAgo;
        private LocalDateTime createdAt;
    }
}
