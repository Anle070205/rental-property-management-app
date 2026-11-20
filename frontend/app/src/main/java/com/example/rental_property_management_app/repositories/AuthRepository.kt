package com.example.rental_property_management_app.repositories

import com.example.rental_property_management_app.models.ActivateAccountRequest
import com.example.rental_property_management_app.models.ApiResponse
import com.example.rental_property_management_app.models.AuthResponse
import com.example.rental_property_management_app.models.ForgotPasswordRequest
import com.example.rental_property_management_app.models.LoginRequest
import com.example.rental_property_management_app.models.RegisterRequest
import com.example.rental_property_management_app.models.ResendOtpRequest
import com.example.rental_property_management_app.models.ResetPasswordRequest
import com.example.rental_property_management_app.models.VerifyOtpRequest
import com.example.rental_property_management_app.models.VerifyResetOtpRequest
import com.example.rental_property_management_app.models.VerifyResetOtpResponse
import com.example.rental_property_management_app.network.RetrofitClient
import com.example.rental_property_management_app.utils.Constants
import com.example.rental_property_management_app.utils.Resource
import com.google.gson.Gson
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

data class UnverifiedChallenge(
    val channel: String,
    val target: String
)

data class AuthCallOutcome(
    val resource: Resource<AuthResponse>,
    val unverifiedChallenge: UnverifiedChallenge? = null
)

/**
 * Auth Repository - Xử lý authentication & OTP (Email / Phone) API calls
 */
class AuthRepository {

    private val apiService = RetrofitClient.apiService
    private val gson = Gson()

    suspend fun testApi(): Resource<Map<String, Any>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.testApi()
            if (response.isSuccessful && response.body() != null) {
                Resource.Success(response.body()!!)
            } else {
                Resource.Error(Constants.ErrorMessages.SERVER_ERROR)
            }
        } catch (_: IOException) {
            Resource.Error(Constants.ErrorMessages.NETWORK_ERROR)
        } catch (e: Exception) {
            Resource.Error(e.message ?: Constants.ErrorMessages.NETWORK_ERROR)
        }
    }

    suspend fun register(
        fullName: String,
        verificationMethod: String,
        email: String?,
        phone: String?,
        password: String,
        role: String = "LANDLORD"
    ): AuthCallOutcome = withContext(Dispatchers.IO) {
        try {
            val request = RegisterRequest(
                fullName = fullName.trim(),
                verificationMethod = verificationMethod,
                email = email?.trim()?.takeIf { it.isNotBlank() },
                phone = phone?.trim()?.takeIf { it.isNotBlank() },
                password = password,
                role = role
            )
            handleAuthOutcome(apiService.register(request))
        } catch (_: IOException) {
            AuthCallOutcome(Resource.Error("Không thể kết nối máy chủ RoomCare. Vui lòng kiểm tra mạng."))
        } catch (e: Exception) {
            AuthCallOutcome(Resource.Error(e.message ?: Constants.ErrorMessages.NETWORK_ERROR))
        }
    }

    suspend fun verifyOtp(channel: String, target: String, otp: String): Resource<String> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.verifyOtp(VerifyOtpRequest(channel, target.trim(), otp.trim()))
                handleUnitMessageResponse(response, "Xác thực tài khoản thành công")
            } catch (_: IOException) {
                Resource.Error("Không thể kết nối máy chủ RoomCare. Vui lòng kiểm tra mạng.")
            } catch (e: Exception) {
                Resource.Error(e.message ?: Constants.ErrorMessages.NETWORK_ERROR)
            }
        }

    suspend fun resendOtp(channel: String, target: String): Resource<String> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.resendOtp(ResendOtpRequest(channel, target.trim()))
                handleUnitMessageResponse(response, "Đã gửi lại mã OTP mới")
            } catch (_: IOException) {
                Resource.Error("Không thể kết nối máy chủ RoomCare. Vui lòng kiểm tra mạng.")
            } catch (e: Exception) {
                Resource.Error(e.message ?: Constants.ErrorMessages.NETWORK_ERROR)
            }
        }

    suspend fun login(
        emailOrPhone: String,
        password: String
    ): AuthCallOutcome = withContext(Dispatchers.IO) {
        try {
            val request = LoginRequest(emailOrPhone.trim(), password)
            handleAuthOutcome(apiService.login(request))
        } catch (_: IOException) {
            AuthCallOutcome(Resource.Error("Không thể kết nối máy chủ RoomCare. Vui lòng kiểm tra mạng."))
        } catch (e: Exception) {
            AuthCallOutcome(Resource.Error(e.message ?: Constants.ErrorMessages.NETWORK_ERROR))
        }
    }

    suspend fun logout(): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.logout()
            if (response.isSuccessful) {
                Resource.Success(Unit)
            } else {
                Resource.Error(Constants.ErrorMessages.SERVER_ERROR)
            }
        } catch (_: Exception) {
            Resource.Error(Constants.ErrorMessages.NETWORK_ERROR)
        }
    }

    suspend fun forgotPassword(channel: String, target: String): Resource<String> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.forgotPassword(ForgotPasswordRequest(channel, target.trim()))
                handleUnitMessageResponse(
                    response,
                    "Nếu tài khoản tồn tại, RoomCare đã gửi mã OTP khôi phục mật khẩu."
                )
            } catch (_: IOException) {
                Resource.Error("Không thể kết nối máy chủ RoomCare. Vui lòng kiểm tra mạng.")
            } catch (e: Exception) {
                Resource.Error(e.message ?: Constants.ErrorMessages.NETWORK_ERROR)
            }
        }

    suspend fun verifyResetOtp(channel: String, target: String, otp: String): Resource<VerifyResetOtpResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.verifyResetOtp(
                    VerifyResetOtpRequest(channel, target.trim(), otp.trim())
                )
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    if (body.success && body.data != null) {
                        Resource.Success(body.data)
                    } else {
                        Resource.Error(body.message)
                    }
                } else {
                    Resource.Error(parseErrorPayload(response).message)
                }
            } catch (_: IOException) {
                Resource.Error("Không thể kết nối máy chủ RoomCare. Vui lòng kiểm tra mạng.")
            } catch (e: Exception) {
                Resource.Error(e.message ?: Constants.ErrorMessages.NETWORK_ERROR)
            }
        }

    suspend fun resetPassword(
        channel: String,
        target: String,
        resetToken: String,
        newPassword: String,
        confirmPassword: String
    ): Resource<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.resetPassword(
                ResetPasswordRequest(channel, target.trim(), resetToken.trim(), newPassword, confirmPassword)
            )
            handleUnitMessageResponse(response, "Đặt lại mật khẩu thành công. Vui lòng đăng nhập lại.")
        } catch (_: IOException) {
            Resource.Error("Không thể kết nối máy chủ RoomCare. Vui lòng kiểm tra mạng.")
        } catch (e: Exception) {
            Resource.Error(e.message ?: Constants.ErrorMessages.NETWORK_ERROR)
        }
    }

    suspend fun activateAccount(
        channel: String,
        target: String,
        otp: String,
        newPassword: String,
        confirmPassword: String
    ): Resource<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.activateAccount(
                ActivateAccountRequest(channel, target.trim(), otp.trim(), newPassword, confirmPassword)
            )
            handleUnitMessageResponse(response, "Kích hoạt tài khoản thành công! Vui lòng đăng nhập.")
        } catch (_: IOException) {
            Resource.Error("Không thể kết nối máy chủ RoomCare. Vui lòng kiểm tra mạng.")
        } catch (e: Exception) {
            Resource.Error(e.message ?: Constants.ErrorMessages.NETWORK_ERROR)
        }
    }

    private fun handleAuthOutcome(
        response: Response<ApiResponse<AuthResponse>>
    ): AuthCallOutcome {
        return if (response.isSuccessful && response.body() != null) {
            val apiResponse = response.body()!!
            if (apiResponse.success && apiResponse.data != null) {
                AuthCallOutcome(Resource.Success(apiResponse.data))
            } else {
                AuthCallOutcome(Resource.Error(apiResponse.message))
            }
        } else {
            val parsed = parseErrorPayload(response)
            AuthCallOutcome(Resource.Error(parsed.message), parsed.challenge)
        }
    }

    private fun handleUnitMessageResponse(
        response: Response<ApiResponse<Unit>>,
        fallbackSuccess: String
    ): Resource<String> {
        return if (response.isSuccessful && response.body() != null) {
            val body = response.body()!!
            if (body.success) {
                Resource.Success(body.message.ifBlank { fallbackSuccess })
            } else {
                Resource.Error(body.message)
            }
        } else {
            Resource.Error(parseErrorPayload(response).message)
        }
    }

    private data class ParsedError(
        val message: String,
        val challenge: UnverifiedChallenge? = null
    )

    private fun parseErrorPayload(response: Response<*>): ParsedError {
        val raw = try {
            response.errorBody()?.string()
        } catch (_: Exception) {
            null
        }
        if (!raw.isNullOrBlank()) {
            try {
                val map = gson.fromJson(raw, Map::class.java)
                val message = map?.get("message")?.toString()?.takeIf { it.isNotBlank() }
                val dataMap = map?.get("data") as? Map<*, *>
                if (dataMap != null) {
                    val requiresVerification = dataMap["requiresVerification"] as? Boolean == true
                    val channel = dataMap["verificationChannel"]?.toString()?.takeIf { it.isNotBlank() }
                    val target = dataMap["verificationTarget"]?.toString()?.takeIf { it.isNotBlank() }
                    if (requiresVerification && channel != null && target != null) {
                        return ParsedError(
                            message ?: "Tài khoản chưa xác thực OTP.",
                            UnverifiedChallenge(channel, target)
                        )
                    }
                    if (dataMap.containsKey("retryAfterSeconds")) {
                        return ParsedError(message ?: "Bạn thao tác quá nhanh. Vui lòng thử lại sau.")
                    }
                    val fieldErrors = dataMap.values
                        .mapNotNull { it?.toString()?.takeIf(String::isNotBlank) }
                        .joinToString("; ")
                    if (fieldErrors.isNotBlank()) {
                        return ParsedError(if (message != null) "$message: $fieldErrors" else fieldErrors)
                    }
                }
                if (message != null) {
                    return ParsedError(message)
                }
            } catch (_: Exception) {
                // Fallback below
            }
        }
        return ParsedError(ApiResultHandler.extractErrorMessage(response))
    }
}
