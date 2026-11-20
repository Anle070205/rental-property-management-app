package com.roomcare.backend.exception;

import lombok.Getter;

/**
 * Exception trả về HTTP 429 khi gửi yêu cầu OTP quá nhanh hoặc vượt giới hạn theo giờ.
 */
@Getter
public class TooManyRequestsException extends RuntimeException {

    private final long retryAfterSeconds;

    public TooManyRequestsException(String message, long retryAfterSeconds) {
        super(message);
        this.retryAfterSeconds = Math.max(1L, retryAfterSeconds);
    }
}
