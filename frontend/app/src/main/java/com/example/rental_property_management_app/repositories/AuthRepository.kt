package com.example.rental_property_management_app.repositories

import com.example.rental_property_management_app.models.ApiResponse
import com.example.rental_property_management_app.models.AuthResponse
import com.example.rental_property_management_app.models.ForgotPasswordRequest
import com.example.rental_property_management_app.models.LoginRequest
import com.example.rental_property_management_app.models.RegisterRequest
import com.example.rental_property_management_app.models.ResendVerificationRequest
import com.example.rental_property_management_app.models.ResetPasswordRequest
import com.example.rental_property_management_app.models.VerifyEmailRequest
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

data class AuthCallOutcome(
    val resource: Resource<AuthResponse>,
    val unverifiedEmail: String? = null
)

/**
 * Auth Repository - Xử lý authentication & Gmail verification/reset API calls
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
        } catch (e: IOException) {
            Resource.Error(Constants.ErrorMessages.NETWORK_ERROR)
        } catch (e: Exception) {
            Resource.Error(e.message ?: Constants.ErrorMessages.NETWORK_ERROR)
        }
    }

    suspend fun register(
        fullName: String,
        email: String,
        phone: String,
        password: String,
        role: String = "LANDLORD"
    ): AuthCallOutcome = withContext(Dispatchers.IO) {
        try {
            val request = RegisterRequest(fullName, email.trim(), phone.trim(), password, role)
            val response = apiService.register(request)
            handleAuthOutcome(response)
        } catch (e: IOException) {
            AuthCallOutcome(Resource.Error("Không thể kết nối máy chủ RoomCare. Vui lòng kiểm tra mạng."))
        } catch (e: Exception) {
            AuthCallOutcome(Resource.Error(e.message ?: Constants.ErrorMessages.NETWORK_ERROR))
        }
    }

    suspend fun verifyEmail(email: String, otp: String): Resource<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.verifyEmail(VerifyEmailRequest(email.trim(), otp.trim()))
            handleUnitMessageResponse(response, "Xác thực Gmail thành công")
        } catch (e: IOException) {
            Resource.Error("Không thể kết nối máy chủ RoomCare. Vui lòng kiểm tra mạng.")
        } catch (e: Exception) {
            Resource.Error(e.message ?: Constants.ErrorMessages.NETWORK_ERROR)
        }
    }

    suspend fun resendVerification(email: String): Resource<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.resendVerification(ResendVerificationRequest(email.trim()))
            handleUnitMessageResponse(response, "Đã gửi lại mã xác thực tới Gmail của bạn")
        } catch (e: IOException) {
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
            val response = apiService.login(request)
            handleAuthOutcome(response)
        } catch (e: IOException) {
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

    suspend fun forgotPassword(email: String): Resource<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.forgotPassword(ForgotPasswordRequest(email.trim()))
            handleUnitMessageResponse(response, "Nếu email tồn tại, mã OTP đã được gửi đến hộp thư Gmail của bạn.")
        } catch (e: IOException) {
            Resource.Error("Không thể kết nối máy chủ RoomCare. Vui lòng kiểm tra mạng.")
        } catch (e: Exception) {
            Resource.Error(e.message ?: Constants.ErrorMessages.NETWORK_ERROR)
        }
    }

    suspend fun verifyResetOtp(email: String, otp: String): Resource<VerifyResetOtpResponse> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.verifyResetOtp(VerifyResetOtpRequest(email.trim(), otp.trim()))
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
            } catch (e: IOException) {
                Resource.Error("Không thể kết nối máy chủ RoomCare. Vui lòng kiểm tra mạng.")
            } catch (e: Exception) {
                Resource.Error(e.message ?: Constants.ErrorMessages.NETWORK_ERROR)
            }
        }

    suspend fun resetPassword(
        email: String,
        resetToken: String,
        newPassword: String,
        confirmPassword: String
    ): Resource<String> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.resetPassword(
                ResetPasswordRequest(email.trim(), resetToken.trim(), newPassword, confirmPassword)
            )
            handleUnitMessageResponse(response, "Đặt lại mật khẩu thành công. Vui lòng đăng nhập lại.")
        } catch (e: IOException) {
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
            AuthCallOutcome(Resource.Error(parsed.message), parsed.unverifiedEmail)
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
        val unverifiedEmail: String? = null
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
                    val requiresVerification = dataMap["requiresEmailVerification"] as? Boolean == true
                    val unverifiedEmail = dataMap["email"]?.toString()?.takeIf { it.isNotBlank() }
                    if (requiresVerification) {
                        return ParsedError(
                            message ?: "Tài khoản chưa xác thực Gmail.",
                            unverifiedEmail
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
