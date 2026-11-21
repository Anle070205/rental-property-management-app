package com.example.rental_property_management_app.utils

import android.util.Patterns

/**
 * Utility class để validate input data
 */
object ValidationUtils {

    private val EMAIL_FALLBACK_REGEX =
        "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$".toRegex()
    private val GMAIL_REGEX =
        "^[A-Za-z0-9._%+-]+@gmail\\.com$".toRegex(RegexOption.IGNORE_CASE)
    private val VN_PHONE_LOCAL_REGEX = "^0([35789]\\d{8})$".toRegex()
    private val VN_PHONE_INTL_REGEX = "^(?:\\+?84)([35789]\\d{8})$".toRegex()
    private val OTP_REGEX = "^\\d{6}$".toRegex()

    /**
     * Validate email format (JVM-safe fallback when Android Patterns is unavailable)
     */
    fun isValidEmail(email: String): Boolean {
        val trimmed = email.trim()
        if (trimmed.isBlank()) return false
        val pattern = Patterns.EMAIL_ADDRESS
        return pattern?.matcher(trimmed)?.matches() ?: EMAIL_FALLBACK_REGEX.matches(trimmed)
    }

    /**
     * Validate personal Gmail address (@gmail.com)
     */
    fun isValidGmail(email: String): Boolean {
        val trimmed = email.trim()
        return trimmed.isNotBlank() && isValidEmail(trimmed) && GMAIL_REGEX.matches(trimmed)
    }

    /**
     * Chuẩn hóa số điện thoại di động Việt Nam sang +84xxxxxxxxx; trả null nếu không hợp lệ
     */
    fun normalizeVietnamPhoneToE164(phone: String): String? {
        val sanitized = phone.trim().replace("[\\s.\\-()]".toRegex(), "")
        if (sanitized.isBlank()) return null
        val localMatch = VN_PHONE_LOCAL_REGEX.matchEntire(sanitized)
        if (localMatch != null) {
            return "+84${localMatch.groupValues[1]}"
        }
        val intlMatch = VN_PHONE_INTL_REGEX.matchEntire(sanitized)
        if (intlMatch != null) {
            return "+84${intlMatch.groupValues[1]}"
        }
        return null
    }

    /**
     * Validate Vietnamese mobile number (0[35789]xxxxxxxx, 84..., +84...)
     */
    fun isValidVietnamPhone(phone: String): Boolean {
        return normalizeVietnamPhoneToE164(phone) != null
    }

    /**
     * Validate phone number (hỗ trợ cả định dạng nội địa 0... và quốc tế +84...)
     */
    fun isValidPhone(phone: String): Boolean {
        return isValidVietnamPhone(phone) || "^0\\d{9}$".toRegex().matches(phone.trim())
    }

    /**
     * Validate 6-digit OTP code
     */
    fun isValidOtp(otp: String): Boolean {
        return OTP_REGEX.matches(otp.trim())
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
     * Get validation error message for Gmail registration
     */
    fun getGmailError(email: String): String? {
        return when {
            email.isBlank() -> "Địa chỉ Gmail không được để trống"
            !isValidEmail(email) -> "Địa chỉ email không hợp lệ"
            !isValidGmail(email) -> "Vui lòng nhập địa chỉ Gmail (@gmail.com) đang hoạt động"
            else -> null
        }
    }

    /**
     * Get validation error message for Vietnamese mobile number
     */
    fun getVietnamPhoneError(phone: String): String? {
        return when {
            phone.isBlank() -> "Số điện thoại không được để trống"
            !isValidVietnamPhone(phone) -> "Số điện thoại Việt Nam không hợp lệ (ví dụ: 0912345678 hoặc +84912345678)"
            else -> null
        }
    }

    /**
     * Get validation error message for 6-digit OTP
     */
    fun getOtpError(otp: String): String? {
        return when {
            otp.isBlank() -> "Vui lòng nhập đầy đủ mã OTP"
            !isValidOtp(otp) -> "Mã OTP phải gồm đúng 6 chữ số"
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
