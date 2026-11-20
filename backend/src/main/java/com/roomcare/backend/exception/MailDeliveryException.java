package com.roomcare.backend.exception;

/**
 * Exception cho lỗi cấu hình SMTP hoặc gửi email xác thực / khôi phục thất bại.
 */
public class MailDeliveryException extends RuntimeException {

    public MailDeliveryException(String message) {
        super(message);
    }

    public MailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }
}
