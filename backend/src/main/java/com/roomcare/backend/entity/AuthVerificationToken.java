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
 * Lưu lịch sử phát hành, băm SHA-256 OTP và reset ticket theo phương thức (EMAIL / PHONE),
 * mục đích (REGISTRATION / PASSWORD_RESET / CONTACT_LINK), IP và số lần nhập sai.
 */
@Entity
@Table(
        name = "auth_verification_tokens",
        indexes = {
                @Index(name = "idx_otp_target_channel_purpose_created", columnList = "target, channel, purpose, created_at"),
                @Index(name = "idx_otp_user_channel_purpose", columnList = "user_id, channel, purpose"),
                @Index(name = "idx_otp_ip_channel_created", columnList = "client_ip, channel, created_at")
        }
)
@Getter
@Setter
@NoArgsConstructor
@ToString(exclude = "user")
@EqualsAndHashCode(of = "id")
public class AuthVerificationToken {

    public enum Channel {
        EMAIL,
        PHONE
    }

    public enum Purpose {
        REGISTRATION,
        PASSWORD_RESET,
        CONTACT_LINK
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Channel channel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Purpose purpose;

    @Column(nullable = false, length = 100)
    private String target;

    @Column(name = "code_hash", nullable = false, length = 64)
    private String codeHash;

    @Column(name = "reset_ticket_hash", length = 64)
    private String resetTicketHash;

    @Column(nullable = false, length = 30)
    private String provider = "LOCAL_HASH";

    @Column(name = "provider_reference", length = 120)
    private String providerReference;

    @Column(name = "client_ip", length = 64)
    private String clientIp;

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
        if (provider == null || provider.isBlank()) {
            provider = "LOCAL_HASH";
        }
    }
}
