package com.example.rental_property_management_app.models

data class ProfileResponse(val fullName: String, val email: String, val phone: String, val role: String)
data class UpdateProfileRequest(val fullName: String, val phone: String)
data class ChangePasswordRequest(val currentPassword: String, val newPassword: String)
