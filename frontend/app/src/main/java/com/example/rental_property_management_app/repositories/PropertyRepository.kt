package com.example.rental_property_management_app.repositories

import com.example.rental_property_management_app.models.*
import com.example.rental_property_management_app.network.RetrofitClient
import com.example.rental_property_management_app.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

/**
 * Property Repository
 * Xử lý data layer cho Property
 */
class PropertyRepository {
    
    private val apiService = RetrofitClient.apiService
    
    /**
     * Get all properties với pagination
     */
    suspend fun getProperties(
        page: Int = 0,
        size: Int = 10,
        sortBy: String = "createdAt",
        sortDir: String = "desc"
    ): Resource<PageResponse<PropertyResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getProperties(page, size, sortBy, sortDir)
            handleApiResponse(response)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Lỗi kết nối")
        }
    }
    
    /**
     * Get property by ID
     */
    suspend fun getPropertyById(id: Long): Resource<PropertyResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getPropertyById(id)
            handleApiResponse(response)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Lỗi kết nối")
        }
    }
    
    /**
     * Create new property
     */
    suspend fun createProperty(request: CreatePropertyRequest): Resource<PropertyResponse> = 
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.createProperty(request)
                handleApiResponse(response)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Lỗi kết nối")
            }
        }
    
    /**
     * Update property
     */
    suspend fun updateProperty(id: Long, request: UpdatePropertyRequest): Resource<PropertyResponse> = 
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.updateProperty(id, request)
                handleApiResponse(response)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Lỗi kết nối")
            }
        }
    
    /**
     * Helper function để xử lý API response
     */
    private fun <T> handleApiResponse(response: Response<ApiResponse<T>>): Resource<T> {
        return if (response.isSuccessful) {
            val apiResponse = response.body()
            if (apiResponse?.success == true && apiResponse.data != null) {
                Resource.Success(apiResponse.data)
            } else {
                Resource.Error(apiResponse?.message ?: "Lỗi không xác định")
            }
        } else {
            Resource.Error(ApiResultHandler.extractErrorMessage(response))
        }
    }
}
