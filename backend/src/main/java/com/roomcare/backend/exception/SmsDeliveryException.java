package com.roomcare.backend.exception;

/**
 * Exception trả về HTTP 503 khi chưa cấu hình SMS OTP provider hoặc gửi SMS thất bại.
 */
public class SmsDeliveryException extends RuntimeException {

    public SmsDeliveryException(String message) {
        super(message);
    }

    public SmsDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
