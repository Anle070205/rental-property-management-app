package com.example.rental_property_management_app.repositories

import com.example.rental_property_management_app.models.*
import com.example.rental_property_management_app.network.RetrofitClient
import com.example.rental_property_management_app.utils.Constants
import com.example.rental_property_management_app.utils.Resource
import java.io.IOException
import org.json.JSONObject
import retrofit2.Response

class UserRepository {
    private val api = RetrofitClient.apiService

    suspend fun profile() = apiResult({ api.getProfile() }) { it.data }
    suspend fun update(request: UpdateProfileRequest) = apiResult({ api.updateProfile(request) }) { it.data }
    suspend fun changePassword(request: ChangePasswordRequest) =
        apiResult({ api.changePassword(request) }) { Unit }

    private suspend fun <T, R : Any> apiResult(
        request: suspend () -> Response<ApiResponse<T>>,
        data: (ApiResponse<T>) -> R?
    ): Resource<R> {
        return try {
            val response = request()
            val body = response.body()
            if (response.isSuccessful && body?.success == true) {
                data(body)?.let { Resource.Success(it) }
                    ?: Resource.Error(Constants.ErrorMessages.SERVER_ERROR)
            } else {
                val message = body?.message ?: runCatching {
                    response.errorBody()?.use { error ->
                        JSONObject(error.string()).optString("message").takeIf { it.isNotBlank() }
                    }
                }.getOrNull()
                Resource.Error(message ?: Constants.ErrorMessages.SERVER_ERROR)
            }
        } catch (_: IOException) {
            Resource.Error(Constants.ErrorMessages.NETWORK_ERROR)
        }
    }
}
