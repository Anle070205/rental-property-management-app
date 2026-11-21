package com.example.rental_property_management_app.models

import com.google.gson.annotations.SerializedName

enum class VerificationChannel {
    @SerializedName("EMAIL")
    EMAIL,

    @SerializedName("PHONE")
    PHONE
}

/**
 * Register Request - Hỗ trợ chọn EMAIL (chỉ Gmail) hoặc PHONE (chỉ SĐT Việt Nam)
 */
data class RegisterRequest(
    @SerializedName("fullName")
    val fullName: String,

    @SerializedName("verificationMethod")
    val verificationMethod: String = "EMAIL",

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("phone")
    val phone: String? = null,

    @SerializedName("password")
    val password: String,

    @SerializedName("role")
    val role: String = "LANDLORD"
)

/**
 * Login Request
 */
data class LoginRequest(
    @SerializedName("emailOrPhone")
    val emailOrPhone: String,
    
    @SerializedName("password")
    val password: String
)

/**
 * Auth Response (from server)
 */
data class AuthResponse(
    @SerializedName("accessToken")
    val accessToken: String? = null,
    
    @SerializedName("tokenType")
    val tokenType: String? = null,
    
    @SerializedName("userId")
    val userId: Long,
    
    @SerializedName("fullName")
    val fullName: String,
    
    @SerializedName("email")
    val email: String? = null,

    @SerializedName("phone")
    val phone: String? = null,
    
    @SerializedName("role")
    val role: String,

    @SerializedName("emailVerified")
    val emailVerified: Boolean = true,

    @SerializedName("phoneVerified")
    val phoneVerified: Boolean = true,

    @SerializedName("requiresVerification")
    val requiresVerification: Boolean = false,

    @SerializedName("verificationChannel")
    val verificationChannel: String? = null,

    @SerializedName("verificationTarget")
    val verificationTarget: String? = null,

    @SerializedName("resendCooldownSeconds")
    val resendCooldownSeconds: Int? = null
)

data class VerifyOtpRequest(
    @SerializedName("channel")
    val channel: String,

    @SerializedName("target")
    val target: String,

    @SerializedName("otp")
    val otp: String
)

data class ResendOtpRequest(
    @SerializedName("channel")
    val channel: String,

    @SerializedName("target")
    val target: String
)

data class SwitchRegistrationChannelRequest(
    @SerializedName("currentChannel")
    val currentChannel: String,

    @SerializedName("currentTarget")
    val currentTarget: String,

    @SerializedName("newChannel")
    val newChannel: String
)

data class ForgotPasswordRequest(
    @SerializedName("channel")
    val channel: String,

    @SerializedName("target")
    val target: String
)

data class VerifyResetOtpRequest(
    @SerializedName("channel")
    val channel: String,

    @SerializedName("target")
    val target: String,

    @SerializedName("otp")
    val otp: String
)

data class VerifyResetOtpResponse(
    @SerializedName("channel")
    val channel: String,

    @SerializedName("target")
    val target: String,

    @SerializedName("resetToken")
    val resetToken: String,

    @SerializedName("expiresInMinutes")
    val expiresInMinutes: Int
)

data class ResetPasswordRequest(
    @SerializedName("channel")
    val channel: String,

    @SerializedName("target")
    val target: String,

    @SerializedName("resetToken")
    val resetToken: String,

    @SerializedName("newPassword")
    val newPassword: String,

    @SerializedName("confirmPassword")
    val confirmPassword: String
)

data class ActivateAccountRequest(
    @SerializedName("channel")
    val channel: String,

    @SerializedName("target")
    val target: String,

    @SerializedName("otp")
    val otp: String,

    @SerializedName("newPassword")
    val newPassword: String,

    @SerializedName("confirmPassword")
    val confirmPassword: String
)

/**
 * Generic API Response wrapper
 */
data class ApiResponse<T>(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String,
    
    @SerializedName("data")
    val data: T?
)

/**
 * User model
 */
data class User(
    val id: Long,
    val fullName: String,
    val email: String,
    val phone: String,
    val role: String
)
