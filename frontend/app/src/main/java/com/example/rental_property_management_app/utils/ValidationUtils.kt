package com.example.rental_property_management_app.utils

import android.util.Patterns

/**
 * Utility class để validate input data
 */
object ValidationUtils {
    
    /**
     * Validate email format
     */
    fun isValidEmail(email: String): Boolean {
        return email.isNotBlank() && Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    
    /**
     * Validate phone number (Vietnam format: 10 digits, start with 0)
     */
    fun isValidPhone(phone: String): Boolean {
        val phonePattern = "^0\\d{9}$".toRegex()
        return phone.isNotBlank() && phonePattern.matches(phone)
    }
    
    /**
     * Validate password length
     */
    fun isValidPassword(password: String): Boolean {
        return password.length >= Constants.MIN_PASSWORD_LENGTH
    }
    
    /**
     * Validate password match
     */
    fun isPasswordMatch(password: String, confirmPassword: String): Boolean {
        return password == confirmPassword
    }
    
    /**
     * Validate empty field
     */
    fun isNotEmpty(text: String): Boolean {
        return text.isNotBlank()
    }
    
    /**
     * Validate full name (at least 2 words)
     */
    fun isValidFullName(name: String): Boolean {
        val trimmedName = name.trim()
        return trimmedName.isNotBlank() && trimmedName.split(" ").size >= 2
    }
    
    /**
     * Check if string is valid email or phone
     */
    fun isValidEmailOrPhone(input: String): Boolean {
        return isValidEmail(input) || isValidPhone(input)
    }
    
    /**
     * Get validation error message for email
     */
    fun getEmailError(email: String): String? {
        return when {
            email.isBlank() -> "Email không được để trống"
            !isValidEmail(email) -> "Email không hợp lệ"
            else -> null
        }
    }
    
    /**
     * Get validation error message for phone
     */
    fun getPhoneError(phone: String): String? {
        return when {
            phone.isBlank() -> "Số điện thoại không được để trống"
            !isValidPhone(phone) -> "Số điện thoại không hợp lệ (10 số, bắt đầu bằng 0)"
            else -> null
        }
    }
    
    /**
     * Get validation error message for password
     */
    fun getPasswordError(password: String): String? {
        return when {
            password.isBlank() -> "Mật khẩu không được để trống"
            password.length < Constants.MIN_PASSWORD_LENGTH -> 
                "Mật khẩu phải có ít nhất ${Constants.MIN_PASSWORD_LENGTH} ký tự"
            else -> null
        }
    }
    
    /**
     * Get validation error message for confirm password
     */
    fun getConfirmPasswordError(password: String, confirmPassword: String): String? {
        return when {
            confirmPassword.isBlank() -> "Xác nhận mật khẩu không được để trống"
            !isPasswordMatch(password, confirmPassword) -> "Mật khẩu không khớp"
            else -> null
        }
    }
    
    /**
     * Get validation error message for full name
     */
    fun getFullNameError(name: String): String? {
        return when {
            name.isBlank() -> "Họ tên không được để trống"
            !isValidFullName(name) -> "Vui lòng nhập đầy đủ họ và tên"
            else -> null
        }
    }
    
    /**
     * Get validation error for email or phone login
     */
    fun getEmailOrPhoneError(input: String): String? {
        return when {
            input.isBlank() -> "Email hoặc số điện thoại không được để trống"
            !isValidEmailOrPhone(input) -> "Email hoặc số điện thoại không hợp lệ"
            else -> null
        }
    }
}
