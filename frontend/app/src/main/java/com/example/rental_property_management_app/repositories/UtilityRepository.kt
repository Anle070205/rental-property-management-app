package com.example.rental_property_management_app.repositories

import com.example.rental_property_management_app.models.*
import com.example.rental_property_management_app.network.RetrofitClient
import com.example.rental_property_management_app.utils.Resource

class UtilityRepository {

    private val apiService = RetrofitClient.apiService

    suspend fun getUtilities(
        month: String? = null,
        roomId: Long? = null,
        propertyId: Long? = null
    ): Resource<List<UtilityRecordResponse>> = ApiResultHandler.execute {
        apiService.getUtilities(month, roomId, propertyId)
    }

    suspend fun getPreviousIndex(
        roomId: Long,
        month: String? = null
    ): Resource<PreviousUtilityIndexResponse> = ApiResultHandler.execute {
        apiService.getPreviousUtilityIndex(roomId, month)
    }

    suspend fun createUtility(request: CreateUtilityRequest): Resource<UtilityRecordResponse> =
        ApiResultHandler.execute {
            apiService.createUtility(request)
        }

    suspend fun updateUtility(
        id: Long,
        request: UpdateUtilityRequest
    ): Resource<UtilityRecordResponse> = ApiResultHandler.execute {
        apiService.updateUtility(id, request)
    }
}
