package com.example.rental_property_management_app.models

import com.google.gson.annotations.SerializedName

/**
 * Register Request
 */
data class RegisterRequest(
    @SerializedName("fullName")
    val fullName: String,
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("phone")
    val phone: String,
    
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
    val accessToken: String,
    
    @SerializedName("tokenType")
    val tokenType: String,
    
    @SerializedName("userId")
    val userId: Long,
    
    @SerializedName("fullName")
    val fullName: String,
    
    @SerializedName("email")
    val email: String,
    
    @SerializedName("role")
    val role: String
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
