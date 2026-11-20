package com.roomcare.backend.exception;

/**
 * Exception trả về HTTP 401 khi thông tin đăng nhập không chính xác.
 */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException(String message) {
        super(message);
    }
}
