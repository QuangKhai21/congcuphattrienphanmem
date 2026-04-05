package com.db7.j2ee_quanlythucung.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    private static final int EXPIRATION_HOURS = 24;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expirationDate;

    @Column(nullable = false)
    @Builder.Default
    private Boolean used = false;

    @PrePersist
    protected void onCreate() {
        if (expirationDate == null) {
            expirationDate = LocalDateTime.now().plusHours(EXPIRATION_HOURS);
        }
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expirationDate);
    }

    public static int getExpirationHours() {
        return EXPIRATION_HOURS;
    }
}
