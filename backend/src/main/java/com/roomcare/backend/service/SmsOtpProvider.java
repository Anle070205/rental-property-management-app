package com.roomcare.backend.service;

/**
 * Hợp đồng nhà cung cấp SMS OTP thật cho số điện thoại Việt Nam (+84).
 */
public interface SmsOtpProvider {

    boolean isConfigured();

    /**
     * true nếu nhà cung cấp tự sinh và xác minh OTP (ví dụ Twilio Verify API).
     * false nếu Backend tự sinh OTP, băm SHA-256 và gửi nội dung SMS qua API.
     */
    boolean isProviderManagedVerification();

    String providerName();

    String sendOtp(String e164Phone, String otpCodeOrNull, String purposeLabel, int expirationMinutes);

    boolean verifyProviderOtp(String e164Phone, String otpCode);
}
