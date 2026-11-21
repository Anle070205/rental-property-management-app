package com.example.rental_property_management_app.models

data class ProfileResponse(
    val fullName: String,
    val email: String?,
    val phone: String?,
    val role: String,
    val emailVerified: Boolean = true,
    val phoneVerified: Boolean = true
)

data class UpdateProfileRequest(val fullName: String, val phone: String?)
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)
data class RequestContactOtpRequest(val channel: String, val target: String)
data class VerifyContactOtpRequest(val channel: String, val target: String, val otp: String)
