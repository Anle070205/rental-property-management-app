package com.roomcare.backend.exception;

import com.roomcare.backend.entity.AuthVerificationToken;
import lombok.Getter;

/**
 * Exception trả về HTTP 403 khi tài khoản đã tồn tại nhưng chưa xác thực qua phương thức đã chọn.
 */
@Getter
public class UnverifiedAccountException extends RuntimeException {

    private final AuthVerificationToken.Channel verificationChannel;
    private final String verificationTarget;

    public UnverifiedAccountException(
            String message,
            AuthVerificationToken.Channel verificationChannel,
            String verificationTarget
    ) {
        super(message);
        this.verificationChannel = verificationChannel;
        this.verificationTarget = verificationTarget;
    }
}
