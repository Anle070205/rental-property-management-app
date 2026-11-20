package com.roomcare.backend.entity;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Lưu mã OTP xác thực Gmail và khôi phục mật khẩu dưới dạng băm SHA-256.
 */
@Entity
@Table(
        name = "auth_verification_tokens",
        indexes = {
                @Index(name = "idx_auth_token_email_purpose_created", columnList = "email, purpose, created_at"),
                @Index(name = "idx_auth_token_user_purpose", columnList = "user_id, purpose")
        }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "user")
@EqualsAndHashCode(of = "id")
public class AuthVerificationToken {

    public enum Purpose {
        EMAIL_VERIFICATION,
        PASSWORD_RESET
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 100)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Purpose purpose;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "reset_ticket_hash", length = 64)
    private String resetTicketHash;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "attempt_count", nullable = false)
    private Integer attemptCount = 0;

    @Column(name = "max_attempts", nullable = false)
    private Integer maxAttempts = 5;

    @Column(name = "used_at")
    private LocalDateTime usedAt;

    @Column(nullable = false)
    private Boolean invalidated = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersistDefaults() {
        if (attemptCount == null) {
            attemptCount = 0;
        }
        if (maxAttempts == null || maxAttempts <= 0) {
            maxAttempts = 5;
        }
        if (invalidated == null) {
            invalidated = false;
        }
    }
}
