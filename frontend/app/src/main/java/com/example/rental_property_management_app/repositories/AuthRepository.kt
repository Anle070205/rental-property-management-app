package com.example.rental_property_management_app.repositories

import com.example.rental_property_management_app.models.ApiResponse
import com.example.rental_property_management_app.models.AuthResponse
import com.example.rental_property_management_app.models.LoginRequest
import com.example.rental_property_management_app.models.RegisterRequest
import com.example.rental_property_management_app.network.RetrofitClient
import com.example.rental_property_management_app.utils.Constants
import com.example.rental_property_management_app.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

/**
 * Auth Repository - Xử lý authentication API calls
 */
class AuthRepository {
    
    private val apiService = RetrofitClient.apiService
    
    /**
     * Test API connection
     */
    suspend fun testApi(): Resource<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.testApi()
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error(Constants.ErrorMessages.SERVER_ERROR)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: Constants.ErrorMessages.NETWORK_ERROR)
        }
    }
    
    /**
     * Register new user (LANDLORD only)
     */
    suspend fun register(
        fullName: String,
        email: String,
        phone: String,
        password: String
    ): Resource<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            val request = RegisterRequest(fullName, email, phone, password)
            val response = apiService.register(request)
            
            handleAuthResponse(response)
        } catch (e: Exception) {
            Resource.Error(e.message ?: Constants.ErrorMessages.NETWORK_ERROR)
        }
    }
    
    /**
     * Login user
     */
    suspend fun login(
        emailOrPhone: String,
        password: String
    ): Resource<AuthResponse> = withContext(Dispatchers.IO) {
        try {
            val request = LoginRequest(emailOrPhone, password)
            val response = apiService.login(request)
            
            handleAuthResponse(response)
        } catch (e: Exception) {
            Resource.Error(e.message ?: Constants.ErrorMessages.NETWORK_ERROR)
        }
    }
    
    /**
     * Logout user
     */
    suspend fun logout(): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.logout()
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error(Constants.ErrorMessages.SERVER_ERROR)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: Constants.ErrorMessages.NETWORK_ERROR)
        }
    }
    
    /**
     * Forgot password
     */
    suspend fun forgotPassword(email: String): Resource<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.forgotPassword(email)
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!.message)
            } else {
                Resource.Error(Constants.ErrorMessages.SERVER_ERROR)
            }
        } catch (e: Exception) {
            Resource.Error(e.message ?: Constants.ErrorMessages.NETWORK_ERROR)
        }
    }
    
    /**
     * Helper function để xử lý auth response
     */
    private fun handleAuthResponse(
        response: Response<ApiResponse<AuthResponse>>
    ): Resource<AuthResponse> {
        return if (response.isSuccessful && response.body() != null) {
            val apiResponse = response.body()!!
            if (apiResponse.success && apiResponse.data != null) {
                Resource.Success(apiResponse.data)
            } else {
                Resource.Error(apiResponse.message)
            }
        } else {
            Resource.Error(Constants.ErrorMessages.SERVER_ERROR)
        }
    }
}
