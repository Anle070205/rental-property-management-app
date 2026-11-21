package com.roomcare.backend.exception;

import lombok.Getter;

/**
 * Exception trả về HTTP 429 khi gửi OTP quá nhanh (cooldown) hoặc vượt hạn mức chống spam.
 */
@Getter
public class TooManyRequestsException extends RuntimeException {

    private final long retryAfterSeconds;

    public TooManyRequestsException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = Math.max(1L, retryAfterSeconds);
    }
}
