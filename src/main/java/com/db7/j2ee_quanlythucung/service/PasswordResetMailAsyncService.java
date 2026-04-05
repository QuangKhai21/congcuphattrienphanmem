package com.db7.j2ee_quanlythucung.service;

import com.db7.j2ee_quanlythucung.entity.PasswordResetToken;
import com.db7.j2ee_quanlythucung.entity.User;
import com.db7.j2ee_quanlythucung.repository.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

/**
 * Gửi mail đặt lại mật khẩu ở luồng nền để request HTTP không chờ SMTP Gmail (thường 2–10 giây).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetMailAsyncService {

    private final JavaMailSender mailSender;
    private final UserRepository userRepository;
    private final TemplateEngine templateEngine;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Async
    public void sendPasswordReset(long userId, String token) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            log.warn("Password reset mail skipped: user id {} not found", userId);
            return;
        }

        String resetUrl = baseUrl + "/reset-password?token=" + token;

        try {
            Context context = new Context();
            context.setVariable("user", user);
            context.setVariable("resetUrl", resetUrl);
            context.setVariable("expirationHours", PasswordResetToken.getExpirationHours());

            String htmlContent = templateEngine.process("email/password-reset", context);

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(user.getEmail());
            helper.setSubject("[Pet Management] Yêu cầu đặt lại mật khẩu");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("Password reset email sent to: {}", user.getEmail());
        } catch (MessagingException e) {
            log.error("Failed to send password reset email to: {}", user.getEmail(), e);
        } catch (org.springframework.mail.MailException e) {
            log.error("SMTP error when sending password reset email to: {}", user.getEmail(), e);
        }
    }
}
