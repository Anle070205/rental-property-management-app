package com.example.rental_property_management_app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rental_property_management_app.RoomCareApplication
import com.example.rental_property_management_app.models.AuthResponse
import com.example.rental_property_management_app.models.VerifyResetOtpResponse
import com.example.rental_property_management_app.repositories.AuthRepository
import com.example.rental_property_management_app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Auth ViewModel - Quản lý authentication, xác thực Gmail và đặt lại mật khẩu
 */
class AuthViewModel : ViewModel() {
    
    private val repository = AuthRepository()
    
    // Register state
    private val _registerState = MutableStateFlow<Resource<AuthResponse>?>(null)
    val registerState: StateFlow<Resource<AuthResponse>?> = _registerState
    
    // Login state
    private val _loginState = MutableStateFlow<Resource<AuthResponse>?>(null)
    val loginState: StateFlow<Resource<AuthResponse>?> = _loginState

    private val _unverifiedEmail = MutableStateFlow<String?>(null)
    val unverifiedEmail: StateFlow<String?> = _unverifiedEmail
    
    // Logout state
    private val _logoutState = MutableStateFlow<Resource<Unit>?>(null)
    val logoutState: StateFlow<Resource<Unit>?> = _logoutState
    
    // Forgot password state
    private val _forgotPasswordState = MutableStateFlow<Resource<String>?>(null)
    val forgotPasswordState: StateFlow<Resource<String>?> = _forgotPasswordState

    // Verify registration Gmail OTP state
    private val _verifyEmailState = MutableStateFlow<Resource<String>?>(null)
    val verifyEmailState: StateFlow<Resource<String>?> = _verifyEmailState

    // Resend OTP state (shared by VerifyEmail & ResetPassword screens)
    private val _resendVerificationState = MutableStateFlow<Resource<String>?>(null)
    val resendVerificationState: StateFlow<Resource<String>?> = _resendVerificationState

    // Verify forgot-password OTP state
    private val _verifyResetOtpState = MutableStateFlow<Resource<VerifyResetOtpResponse>?>(null)
    val verifyResetOtpState: StateFlow<Resource<VerifyResetOtpResponse>?> = _verifyResetOtpState

    // Reset password state
    private val _resetPasswordState = MutableStateFlow<Resource<String>?>(null)
    val resetPasswordState: StateFlow<Resource<String>?> = _resetPasswordState
    
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
            _unverifiedEmail.value = null
            _registerState.value = Resource.Loading()
            
            val outcome = repository.register(fullName, email, phone, password, role)
            _unverifiedEmail.value = outcome.unverifiedEmail
            val result = outcome.resource
            // Chỉ lưu token nếu backend thực sự đã xác thực và cấp accessToken
            if (result is Resource.Success && result.data != null &&
                !result.data.requiresEmailVerification && !result.data.accessToken.isNullOrBlank()
            ) {
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
            _unverifiedEmail.value = null
            _loginState.value = Resource.Loading()
            
            val outcome = repository.login(emailOrPhone, password)
            _unverifiedEmail.value = outcome.unverifiedEmail
            val result = outcome.resource
            if (result is Resource.Success && result.data != null && !result.data.accessToken.isNullOrBlank()) {
                saveUserData(result.data)
            }
            _loginState.value = result
        }
    }
    
    /**
     * Logout user
     */
    fun logout() {
        RoomCareApplication.prefsManager.clearAllData()
        viewModelScope.launch {
            _logoutState.value = Resource.Loading()
            val result = repository.logout()
            _logoutState.value = result
        }
    }

    fun verifyEmail(email: String, otp: String) {
        viewModelScope.launch {
            _verifyEmailState.value = Resource.Loading()
            _verifyEmailState.value = repository.verifyEmail(email, otp)
        }
    }

    fun resendVerification(email: String) {
        viewModelScope.launch {
            _resendVerificationState.value = Resource.Loading()
            _resendVerificationState.value = repository.resendVerification(email)
        }
    }
    
    /**
     * Forgot password
     */
    fun forgotPassword(email: String) {
        viewModelScope.launch {
            _forgotPasswordState.value = Resource.Loading()
            _forgotPasswordState.value = repository.forgotPassword(email)
        }
    }

    fun verifyResetOtp(email: String, otp: String) {
        viewModelScope.launch {
            _verifyResetOtpState.value = Resource.Loading()
            _verifyResetOtpState.value = repository.verifyResetOtp(email, otp)
        }
    }

    fun resetPassword(email: String, resetToken: String, newPassword: String, confirmPassword: String) {
        viewModelScope.launch {
            _resetPasswordState.value = Resource.Loading()
            _resetPasswordState.value = repository.resetPassword(email, resetToken, newPassword, confirmPassword)
        }
    }
    
    /**
     * Save user data to SharedPreferences
     */
    private fun saveUserData(authResponse: AuthResponse) {
        val token = authResponse.accessToken ?: return
        RoomCareApplication.prefsManager.saveUserData(
            accessToken = token,
            refreshToken = null,
            userId = authResponse.userId,
            role = authResponse.role,
            name = authResponse.fullName,
            email = authResponse.email
        )
    }
    
    fun resetRegisterState() {
        _registerState.value = null
        _unverifiedEmail.value = null
    }
    
    fun resetLoginState() {
        _loginState.value = null
        _unverifiedEmail.value = null
    }
    
    fun resetLogoutState() {
        _logoutState.value = null
    }
    
    fun resetForgotPasswordState() {
        _forgotPasswordState.value = null
    }

    fun resetVerifyEmailState() {
        _verifyEmailState.value = null
    }

    fun resetResendVerificationState() {
        _resendVerificationState.value = null
    }

    fun resetVerifyResetOtpState() {
        _verifyResetOtpState.value = null
    }

    fun resetPasswordResetState() {
        _resetPasswordState.value = null
    }
}
