package com.example.rental_property_management_app.network

import com.example.rental_property_management_app.models.ApiResponse
import com.example.rental_property_management_app.models.AuthResponse
import com.example.rental_property_management_app.models.LoginRequest
import com.example.rental_property_management_app.models.RegisterRequest
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API Service Interface
 */
interface ApiService {
    
    // Test API
    @GET("api/test")
    suspend fun testApi(): Response<Map<String, Any>>
    
    // Authentication APIs
    @POST("api/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<ApiResponse<AuthResponse>>
    
    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ApiResponse<AuthResponse>>
    
    @POST("api/auth/logout")
    suspend fun logout(): Response<ApiResponse<Unit>>
    
    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(
        @Query("email") email: String
    ): Response<ApiResponse<Unit>>
}
