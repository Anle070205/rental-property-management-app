package com.example.rental_property_management_app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rental_property_management_app.RoomCareApplication
import com.example.rental_property_management_app.models.AuthResponse
import com.example.rental_property_management_app.repositories.AuthRepository
import com.example.rental_property_management_app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Auth ViewModel - Quản lý authentication state và logic
 */
class AuthViewModel : ViewModel() {
    
    private val repository = AuthRepository()
    
    // Register state
    private val _registerState = MutableStateFlow<Resource<AuthResponse>?>(null)
    val registerState: StateFlow<Resource<AuthResponse>?> = _registerState
    
    // Login state
    private val _loginState = MutableStateFlow<Resource<AuthResponse>?>(null)
    val loginState: StateFlow<Resource<AuthResponse>?> = _loginState
    
    // Logout state
    private val _logoutState = MutableStateFlow<Resource<Unit>?>(null)
    val logoutState: StateFlow<Resource<Unit>?> = _logoutState
    
    // Forgot password state
    private val _forgotPasswordState = MutableStateFlow<Resource<String>?>(null)
    val forgotPasswordState: StateFlow<Resource<String>?> = _forgotPasswordState
    
    /**
     * Register new user (LANDLORD or TENANT)
     */
    fun register(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        role: String = "LANDLORD"
    ) {
        viewModelScope.launch {
            _registerState.value = Resource.Loading()
            
            val result = repository.register(fullName, email, phone, password, role)
            // Save user data before notifying observers so role-based navigation sees persisted prefs
            if (result is Resource.Success && result.data != null) {
                saveUserData(result.data)
            }
            _registerState.value = result
        }
    }
    
    /**
     * Login user
     */
    fun login(emailOrPhone: String, password: String) {
        viewModelScope.launch {
            _loginState.value = Resource.Loading()
            
            val result = repository.login(emailOrPhone, password)
            // Save user data before notifying observers so role-based navigation sees persisted prefs
            if (result is Resource.Success && result.data != null) {
                saveUserData(result.data)
            }
            _loginState.value = result
        }
    }
    
    /**
     * Logout user
     */
    fun logout() {
        viewModelScope.launch {
            _logoutState.value = Resource.Loading()
            
            val result = repository.logout()
            _logoutState.value = result
            
            // Clear user data
            RoomCareApplication.prefsManager.clearAllData()
        }
    }
    
    /**
     * Forgot password
     */
    fun forgotPassword(email: String) {
        viewModelScope.launch {
            _forgotPasswordState.value = Resource.Loading()
            
            val result = repository.forgotPassword(email)
            _forgotPasswordState.value = result
        }
    }
    
    /**
     * Save user data to SharedPreferences
     */
    private fun saveUserData(authResponse: AuthResponse) {
        RoomCareApplication.prefsManager.saveUserData(
            accessToken = authResponse.accessToken,
            refreshToken = null, // TODO: implement refresh token
            userId = authResponse.userId,
            role = authResponse.role,
            name = authResponse.fullName,
            email = authResponse.email
        )
    }
    
    /**
     * Reset register state
     */
    fun resetRegisterState() {
        _registerState.value = null
    }
    
    /**
     * Reset login state
     */
    fun resetLoginState() {
        _loginState.value = null
    }
    
    /**
     * Reset logout state
     */
    fun resetLogoutState() {
        _logoutState.value = null
    }
    
    /**
     * Reset forgot password state
     */
    fun resetForgotPasswordState() {
        _forgotPasswordState.value = null
    }
}
