package com.roomcare.backend.service;

/**
 * Hợp đồng dịch vụ gửi mã OTP xác thực và khôi phục mật khẩu qua email thật.
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

    void sendContactVerificationEmail(
            String toEmail,
            String fullName,
            String otpCode,
            int expirationMinutes
    );

    default void sendAccountActivationEmail(
            String toEmail,
            String fullName,
            String landlordName,
            String roomCode,
            String otpCode,
            int expirationMinutes
    ) {
        sendRegistrationVerificationEmail(toEmail, fullName, otpCode, expirationMinutes, null);
    }
}
