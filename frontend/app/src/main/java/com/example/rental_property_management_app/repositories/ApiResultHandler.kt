package com.example.rental_property_management_app.repositories

import com.example.rental_property_management_app.models.ApiResponse
import com.example.rental_property_management_app.utils.Resource
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

/**
 * Shared helper for Phase 4 repositories to execute Retrofit calls safely and
 * extract detailed Vietnamese error messages from Backend ApiResponse payloads.
 */
object ApiResultHandler {

    private val gson = Gson()

    suspend fun <T> execute(call: suspend () -> Response<ApiResponse<T>>): Resource<T> =
        withContext(Dispatchers.IO) {
            try {
                val response = call()
                handleResponse(response)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Lỗi kết nối máy chủ")
            }
        }

    fun <T> handleResponse(response: Response<ApiResponse<T>>): Resource<T> {
        if (response.isSuccessful) {
            val apiResponse = response.body()
            return if (apiResponse?.success == true && apiResponse.data != null) {
                Resource.Success(apiResponse.data)
            } else {
                Resource.Error(apiResponse?.message ?: "Lỗi không xác định")
            }
        }
        return Resource.Error(extractErrorMessage(response))
    }

    private fun extractErrorMessage(response: Response<*>): String {
        val raw = try {
            response.errorBody()?.string()
        } catch (e: Exception) {
            null
        }
        if (!raw.isNullOrBlank()) {
            try {
                val obj = JsonParser.parseString(raw).asJsonObject
                val message = obj.get("message")?.takeIf { !it.isJsonNull }?.asString
                val dataEl = obj.get("data")
                if (dataEl != null && dataEl.isJsonObject) {
                    val fieldErrors = dataEl.asJsonObject.entrySet()
                        .joinToString("; ") { "${it.key}: ${it.value.asString}" }
                    if (fieldErrors.isNotBlank()) {
                        return if (!message.isNullOrBlank()) "$message ($fieldErrors)" else fieldErrors
                    }
                }
                if (!message.isNullOrBlank()) {
                    return message
                }
            } catch (_: Exception) {
                try {
                    val parsed = gson.fromJson(raw, ApiResponse::class.java)
                    if (!parsed?.message.isNullOrBlank()) {
                        return parsed.message
                    }
                } catch (_: Exception) {
                    // Fall back to HTTP status
                }
            }
        }
        return "HTTP ${response.code()}: ${response.message()}"
    }
}
