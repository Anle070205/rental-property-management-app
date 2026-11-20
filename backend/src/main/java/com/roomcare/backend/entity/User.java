package com.roomcare.backend.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * User Entity - Người dùng trong hệ thống
 */
@Entity
@Table(name = "users")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String fullName;

    @Column(unique = true, length = 100)
    private String email;

    @Column(unique = true, length = 20)
    private String phone;

    @Column(nullable = false, length = 255)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Column(name = "email_verified", nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT TRUE")
    private Boolean emailVerified = true;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "phone_verified", nullable = false, columnDefinition = "BOOLEAN NOT NULL DEFAULT TRUE")
    private Boolean phoneVerified = true;

    @Column(name = "phone_verified_at")
    private LocalDateTime phoneVerifiedAt;

    @Column(name = "token_version", nullable = false, columnDefinition = "INT NOT NULL DEFAULT 0")
    private Integer tokenVersion = 0;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersistDefaults() {
        if (isActive == null) {
            isActive = true;
        }
        if (emailVerified == null) {
            emailVerified = true;
        }
        if (phoneVerified == null) {
            phoneVerified = true;
        }
        if (tokenVersion == null) {
            tokenVersion = 0;
        }
    }

    public boolean isAccountVerified() {
        return Boolean.TRUE.equals(emailVerified) || Boolean.TRUE.equals(phoneVerified);
    }

    public String getPrimaryIdentifier() {
        if (email != null && !email.isBlank()) {
            return email;
        }
        return phone != null ? phone : "";
    }

    // Constructor for registration / legacy compatibility
    public User(String fullName, String email, String phone, String password, Role role) {
        this.fullName = fullName;
        this.email = email;
        this.phone = phone;
        this.password = password;
        this.role = role;
        this.isActive = true;
        this.emailVerified = true;
        this.phoneVerified = true;
        this.tokenVersion = 0;
    }
}
