package com.roomcare.backend.service;

/**
 * Hợp đồng dịch vụ gửi email xác thực và khôi phục mật khẩu cho RoomCare.
 */
public interface EmailService {

    boolean isConfigured();

    void sendRegistrationVerificationEmail(
            String toEmail,
            String fullName,
            String otpCode,
            int expirationMinutes,
            String verificationUrl
    );

    void sendPasswordResetEmail(
            String toEmail,
            String fullName,
            String otpCode,
            int expirationMinutes
    );
}
