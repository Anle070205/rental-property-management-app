package com.roomcare.backend.exception;

import lombok.Getter;

/**
 * Exception trả về HTTP 403 khi tài khoản đã tồn tại nhưng chưa xác thực Gmail.
 */
@Getter
public class UnverifiedAccountException extends RuntimeException {

    private final String email;

    public UnverifiedAccountException(String message, String email) {
        super(message);
        this.email = email;
    }
}
