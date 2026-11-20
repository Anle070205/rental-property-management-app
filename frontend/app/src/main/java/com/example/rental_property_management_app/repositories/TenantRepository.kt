package com.example.rental_property_management_app.repositories

import com.example.rental_property_management_app.models.*
import com.example.rental_property_management_app.network.RetrofitClient
import com.example.rental_property_management_app.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

/**
 * Tenant Repository
 * Xử lý data layer cho Tenant
 */
class TenantRepository {
    
    private val apiService = RetrofitClient.apiService
    
    /**
     * Get tenants với filters
     */
    suspend fun getTenants(
        propertyId: Long? = null,
        keyword: String? = null,
        page: Int = 0,
        size: Int = 10,
        sortBy: String = "fullName",
        sortDir: String = "asc"
    ): Resource<PageResponse<TenantResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getTenants(propertyId, keyword, page, size, sortBy, sortDir)
            handleApiResponse(response)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Lỗi kết nối")
        }
    }
    
    /**
     * Get tenant by ID
     */
    suspend fun getTenantById(id: Long): Resource<TenantResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getTenantById(id)
            handleApiResponse(response)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Lỗi kết nối")
        }
    }
    
    /**
     * Create new tenant
     */
    suspend fun createTenant(request: CreateTenantRequest): Resource<TenantResponse> = 
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.createTenant(request)
                handleApiResponse(response)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Lỗi kết nối")
            }
        }
    
    /**
     * Update tenant
     */
    suspend fun updateTenant(id: Long, request: UpdateTenantRequest): Resource<TenantResponse> = 
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.updateTenant(id, request)
                handleApiResponse(response)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Lỗi kết nối")
            }
        }
    
    /**
     * Delete tenant
     */
    suspend fun deleteTenant(id: Long): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteTenant(id)
            if (response.isSuccessful) {
                val apiResponse = response.body()
                if (response.code() == 204 || apiResponse == null || apiResponse.success) {
                    Resource.Success(Unit)
                } else {
                    Resource.Error(apiResponse.message)
                }
            } else {
                Resource.Error(ApiResultHandler.extractErrorMessage(response))
            }
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
