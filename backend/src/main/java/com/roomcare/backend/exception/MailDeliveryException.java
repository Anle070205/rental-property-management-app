package com.roomcare.backend.exception;

/**
 * Exception trả về HTTP 503 khi chưa cấu hình Gmail SMTP hoặc gửi email xác thực/khôi phục thất bại.
 */
public class MailDeliveryException extends RuntimeException {

    public MailDeliveryException(String message) {
        super(message);
    }

    public MailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
