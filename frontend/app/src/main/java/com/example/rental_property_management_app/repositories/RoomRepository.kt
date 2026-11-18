package com.example.rental_property_management_app.repositories

import com.example.rental_property_management_app.models.*
import com.example.rental_property_management_app.network.RetrofitClient
import com.example.rental_property_management_app.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response

/**
 * Room Repository
 * Xử lý data layer cho Room
 */
class RoomRepository {
    
    private val apiService = RetrofitClient.apiService
    
    /**
     * Get all rooms của landlord
     */
    suspend fun getAllRooms(
        page: Int = 0,
        size: Int = 20,
        sortBy: String = "roomCode",
        sortDir: String = "asc"
    ): Resource<PageResponse<RoomResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getAllRooms(page, size, sortBy, sortDir)
            handleApiResponse(response)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Lỗi kết nối")
        }
    }
    
    /**
     * Get rooms by property với filters
     */
    suspend fun getRoomsByProperty(
        propertyId: Long,
        status: RoomStatus? = null,
        minPrice: String? = null,
        maxPrice: String? = null,
        floor: Int? = null,
        page: Int = 0,
        size: Int = 20,
        sortBy: String = "roomCode",
        sortDir: String = "asc"
    ): Resource<PageResponse<RoomResponse>> = withContext(Dispatchers.IO) {
        try {
            val statusStr = status?.name
            val response = apiService.getRoomsByProperty(
                propertyId, statusStr, minPrice, maxPrice, floor, page, size, sortBy, sortDir
            )
            handleApiResponse(response)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Lỗi kết nối")
        }
    }
    
    /**
     * Get room by ID
     */
    suspend fun getRoomById(id: Long): Resource<RoomResponse> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getRoomById(id)
            handleApiResponse(response)
        } catch (e: Exception) {
            Resource.Error(e.message ?: "Lỗi kết nối")
        }
    }
    
    /**
     * Create new room
     */
    suspend fun createRoom(request: CreateRoomRequest): Resource<RoomResponse> = 
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.createRoom(request)
                handleApiResponse(response)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Lỗi kết nối")
            }
        }
    
    /**
     * Update room
     */
    suspend fun updateRoom(id: Long, request: UpdateRoomRequest): Resource<RoomResponse> = 
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.updateRoom(id, request)
                handleApiResponse(response)
            } catch (e: Exception) {
                Resource.Error(e.message ?: "Lỗi kết nối")
            }
        }
    
    /**
     * Delete room
     */
    suspend fun deleteRoom(id: Long): Resource<Unit> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.deleteRoom(id)
            if (response.isSuccessful) {
                val apiResponse = response.body()
                if (apiResponse?.success == true) {
                    Resource.Success(Unit)
                } else {
                    Resource.Error(apiResponse?.message ?: "Xóa phòng thất bại")
                }
            } else {
                Resource.Error("HTTP ${response.code()}: ${response.message()}")
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
            Resource.Error("HTTP ${response.code()}: ${response.message()}")
        }
    }
}
