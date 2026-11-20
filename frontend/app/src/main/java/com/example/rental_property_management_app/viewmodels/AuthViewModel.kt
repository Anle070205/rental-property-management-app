package com.example.rental_property_management_app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rental_property_management_app.RoomCareApplication
import com.example.rental_property_management_app.models.AuthResponse
import com.example.rental_property_management_app.models.VerifyResetOtpResponse
import com.example.rental_property_management_app.repositories.AuthRepository
import com.example.rental_property_management_app.repositories.UnverifiedChallenge
import com.example.rental_property_management_app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Auth ViewModel - Quản lý trạng thái đăng ký, xác thực OTP (Email / SĐT), đăng nhập & quên mật khẩu
 */
class AuthViewModel : ViewModel() {

    private val repository = AuthRepository()

    private val _registerState = MutableStateFlow<Resource<AuthResponse>?>(null)
    val registerState: StateFlow<Resource<AuthResponse>?> = _registerState

    private val _loginState = MutableStateFlow<Resource<AuthResponse>?>(null)
    val loginState: StateFlow<Resource<AuthResponse>?> = _loginState

    private val _unverifiedChallenge = MutableStateFlow<UnverifiedChallenge?>(null)
    val unverifiedChallenge: StateFlow<UnverifiedChallenge?> = _unverifiedChallenge

    private val _logoutState = MutableStateFlow<Resource<Unit>?>(null)
    val logoutState: StateFlow<Resource<Unit>?> = _logoutState

    private val _forgotPasswordState = MutableStateFlow<Resource<String>?>(null)
    val forgotPasswordState: StateFlow<Resource<String>?> = _forgotPasswordState

    private val _verifyOtpState = MutableStateFlow<Resource<String>?>(null)
    val verifyOtpState: StateFlow<Resource<String>?> = _verifyOtpState

    private val _resendOtpState = MutableStateFlow<Resource<String>?>(null)
    val resendOtpState: StateFlow<Resource<String>?> = _resendOtpState

    private val _verifyResetOtpState = MutableStateFlow<Resource<VerifyResetOtpResponse>?>(null)
    val verifyResetOtpState: StateFlow<Resource<VerifyResetOtpResponse>?> = _verifyResetOtpState

    private val _resetPasswordState = MutableStateFlow<Resource<String>?>(null)
    val resetPasswordState: StateFlow<Resource<String>?> = _resetPasswordState

    // Lưu trạng thái chống mất khi xoay màn hình (configuration change)
    var verificationCooldownEndMs: Long = 0L
    var resetCooldownEndMs: Long = 0L
    var verifiedResetToken: String? = null
    var resetTokenChannel: String? = null
    var resetTokenTarget: String? = null

    fun remainingVerificationCooldownSeconds(): Int {
        val diff = verificationCooldownEndMs - System.currentTimeMillis()
        return if (diff > 0L) ((diff + 999L) / 1000L).toInt() else 0
    }

    fun remainingResetCooldownSeconds(): Int {
        val diff = resetCooldownEndMs - System.currentTimeMillis()
        return if (diff > 0L) ((diff + 999L) / 1000L).toInt() else 0
    }

    fun markVerificationCooldown(seconds: Int) {
        verificationCooldownEndMs = if (seconds > 0) System.currentTimeMillis() + seconds * 1000L else 0L
    }

    fun markResetCooldown(seconds: Int) {
        resetCooldownEndMs = if (seconds > 0) System.currentTimeMillis() + seconds * 1000L else 0L
    }

    fun clearOtpAndResetProgress() {
        verificationCooldownEndMs = 0L
        resetCooldownEndMs = 0L
        verifiedResetToken = null
        resetTokenChannel = null
        resetTokenTarget = null
        _verifyOtpState.value = null
        _resendOtpState.value = null
        _verifyResetOtpState.value = null
        _resetPasswordState.value = null
        _forgotPasswordState.value = null
        _registerState.value = null
        _unverifiedChallenge.value = null
    }

    fun register(
        fullName: String,
        verificationMethod: String,
        email: String?,
        phone: String?,
        password: String,
        role: String = "LANDLORD"
    ) {
        if (_registerState.value is Resource.Loading) return
        viewModelScope.launch {
            _unverifiedChallenge.value = null
            _registerState.value = Resource.Loading()

            val outcome = repository.register(fullName, verificationMethod, email, phone, password, role)
            _unverifiedChallenge.value = outcome.unverifiedChallenge
            val result = outcome.resource
            if (result is Resource.Success && result.data != null) {
                val data = result.data
                if (!data.requiresVerification && !data.accessToken.isNullOrBlank()) {
                    saveUserData(data)
                } else {
                    markVerificationCooldown(data.resendCooldownSeconds ?: 60)
                }
            }
            _registerState.value = result
        }
    }

    fun login(emailOrPhone: String, password: String) {
        if (_loginState.value is Resource.Loading) return
        viewModelScope.launch {
            _unverifiedChallenge.value = null
            _loginState.value = Resource.Loading()

            val outcome = repository.login(emailOrPhone, password)
            _unverifiedChallenge.value = outcome.unverifiedChallenge
            val result = outcome.resource
            if (result is Resource.Success && result.data != null && !result.data.accessToken.isNullOrBlank()) {
                saveUserData(result.data)
            }
            _loginState.value = result
        }
    }

    fun logout() {
        RoomCareApplication.prefsManager.clearAllData()
        viewModelScope.launch {
            _logoutState.value = Resource.Loading()
            _logoutState.value = repository.logout()
        }
    }

    fun verifyOtp(channel: String, target: String, otp: String) {
        if (_verifyOtpState.value is Resource.Loading) return
        viewModelScope.launch {
            _verifyOtpState.value = Resource.Loading()
            _verifyOtpState.value = repository.verifyOtp(channel, target, otp)
        }
    }

    fun resendOtp(channel: String, target: String) {
        if (_resendOtpState.value is Resource.Loading) return
        viewModelScope.launch {
            _resendOtpState.value = Resource.Loading()
            val res = repository.resendOtp(channel, target)
            if (res is Resource.Success) {
                markVerificationCooldown(60)
            }
            _resendOtpState.value = res
        }
    }

    fun forgotPassword(channel: String, target: String) {
        if (_forgotPasswordState.value is Resource.Loading) return
        viewModelScope.launch {
            _forgotPasswordState.value = Resource.Loading()
            val res = repository.forgotPassword(channel, target)
            if (res is Resource.Success) {
                markResetCooldown(60)
            }
            _forgotPasswordState.value = res
        }
    }

    fun verifyResetOtp(channel: String, target: String, otp: String) {
        if (_verifyResetOtpState.value is Resource.Loading) return
        viewModelScope.launch {
            _verifyResetOtpState.value = Resource.Loading()
            val res = repository.verifyResetOtp(channel, target, otp)
            if (res is Resource.Success && res.data != null) {
                verifiedResetToken = res.data.resetToken
                resetTokenChannel = res.data.channel
                resetTokenTarget = res.data.target
            }
            _verifyResetOtpState.value = res
        }
    }

    fun resetPassword(channel: String, target: String, resetToken: String, newPassword: String, confirmPassword: String) {
        if (_resetPasswordState.value is Resource.Loading) return
        viewModelScope.launch {
            _resetPasswordState.value = Resource.Loading()
            _resetPasswordState.value = repository.resetPassword(channel, target, resetToken, newPassword, confirmPassword)
        }
    }

    private fun saveUserData(authResponse: AuthResponse) {
        val token = authResponse.accessToken ?: return
        RoomCareApplication.prefsManager.saveUserData(
            accessToken = token,
            refreshToken = null,
            userId = authResponse.userId,
            role = authResponse.role,
            name = authResponse.fullName,
            email = authResponse.email ?: authResponse.phone
        )
    }

    fun resetRegisterState() {
        _registerState.value = null
        _unverifiedChallenge.value = null
    }

    fun resetLoginState() {
        _loginState.value = null
        _unverifiedChallenge.value = null
    }

    fun resetLogoutState() {
        _logoutState.value = null
    }

    fun resetForgotPasswordState() {
        _forgotPasswordState.value = null
    }

    fun resetVerifyOtpState() {
        _verifyOtpState.value = null
    }

    fun resetResendOtpState() {
        _resendOtpState.value = null
    }

    fun resetVerifyResetOtpState() {
        _verifyResetOtpState.value = null
    }

    fun resetPasswordResetState() {
        _resetPasswordState.value = null
    }
}
