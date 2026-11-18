package com.example.rental_property_management_app.repositories

import com.example.rental_property_management_app.models.HealthScoreOverviewResponse
import com.example.rental_property_management_app.models.RoomHealthScoreResponse
import com.example.rental_property_management_app.network.RetrofitClient
import com.example.rental_property_management_app.utils.Resource

class HealthScoreRepository {

    private val apiService = RetrofitClient.apiService

    suspend fun getHealthScores(
        ratingLevel: String? = null,
        propertyId: Long? = null
    ): Resource<HealthScoreOverviewResponse> = ApiResultHandler.execute {
        apiService.getHealthScores(ratingLevel, propertyId)
    }

    suspend fun getRoomHealthScore(roomId: Long): Resource<RoomHealthScoreResponse> =
        ApiResultHandler.execute {
            apiService.getRoomHealthScore(roomId)
        }
}
