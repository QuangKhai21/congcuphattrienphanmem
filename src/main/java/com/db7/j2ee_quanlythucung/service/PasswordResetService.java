package com.db7.j2ee_quanlythucung.service;

import com.db7.j2ee_quanlythucung.entity.PasswordResetToken;
import com.db7.j2ee_quanlythucung.entity.User;
import com.db7.j2ee_quanlythucung.repository.PasswordResetTokenRepository;
import com.db7.j2ee_quanlythucung.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final PasswordResetTokenRepository tokenRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetMailAsyncService passwordResetMailAsyncService;

    @Value("${app.base-url:http://localhost:8080}")
    private String baseUrl;

    /** Gmail SMTP: From phải trùng (hoặc là alias đã xác minh của) tài khoản đăng nhập SMTP. */
    @Value("${spring.mail.username}")
    private String fromEmail;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${app.email.enabled:true}")
    private boolean emailEnabled;

    /**
     * Tạo token nhưng KHÔNG gửi email — trả về reset URL để hiển thị trực tiếp trên trang.
     * Fallback khi không có SMTP.
     */
    @Transactional
    public String createTokenOnly(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.warn("Password reset requested for unknown email: {}", email);
            return null;
        }

        User user = userOpt.get();
        if (!user.getEnabled()) {
            log.warn("Password reset requested for disabled user: {}", email);
            return null;
        }

        tokenRepository.deleteByUserId(user.getId());

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expirationDate(LocalDateTime.now().plusHours(PasswordResetToken.getExpirationHours()))
                .used(false)
                .build();
        tokenRepository.save(resetToken);

        return baseUrl + "/reset-password?token=" + token;
    }

    @Transactional
    public boolean createTokenAndSendEmail(String email) {
        if (!canSendPasswordResetMail()) {
            return false;
        }

        Optional<User> userOpt = userRepository.findByEmail(email);
        if (userOpt.isEmpty()) {
            log.warn("Password reset requested for unknown email: {}", email);
            return false;
        }

        User user = userOpt.get();
        if (!user.getEnabled()) {
            log.warn("Password reset requested for disabled user: {}", email);
            return false;
        }

        tokenRepository.deleteByUserId(user.getId());

        String token = UUID.randomUUID().toString();
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expirationDate(LocalDateTime.now().plusHours(PasswordResetToken.getExpirationHours()))
                .used(false)
                .build();
        tokenRepository.save(resetToken);

        passwordResetMailAsyncService.sendPasswordReset(user.getId(), token);
        return true;
    }

    /** Kiểm tra nhanh trước khi lưu token — tránh tạo token rồi không gửi được mail. */
    private boolean canSendPasswordResetMail() {
        if (!emailEnabled) {
            log.warn("app.email.enabled=false — không gửi mail quên mật khẩu.");
            return false;
        }
        if (fromEmail == null || fromEmail.isBlank() || fromEmail.contains("YOUR_EMAIL")) {
            log.error("Chưa cấu hình spring.mail.username (email Gmail thật). Không gửi được mail quên mật khẩu.");
            return false;
        }
        if (mailPassword == null || mailPassword.isBlank() || mailPassword.contains("YOUR_APP_PASSWORD")) {
            log.error("Chưa cấu hình spring.mail.password (Mật khẩu ứng dụng Gmail 16 ký tự). Không gửi được mail quên mật khẩu.");
            return false;
        }
        return true;
    }

    @Transactional(readOnly = true)
    public Optional<PasswordResetToken> getToken(String token) {
        return tokenRepository.findByToken(token);
    }

    @Transactional
    public boolean resetPassword(String token, String newPassword) {
        Optional<PasswordResetToken> tokenOpt = tokenRepository.findByToken(token);
        if (tokenOpt.isEmpty()) {
            return false;
        }

        PasswordResetToken resetToken = tokenOpt.get();
        if (resetToken.getUsed() || resetToken.isExpired()) {
            return false;
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);

        log.info("Password reset successful for user: {}", user.getUsername());
        return true;
    }

    @Transactional
    public void cleanupExpiredTokens() {
        tokenRepository.deleteExpiredAndUsedTokens(LocalDateTime.now());
    }
}
