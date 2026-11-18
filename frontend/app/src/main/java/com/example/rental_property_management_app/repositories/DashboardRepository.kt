package com.example.rental_property_management_app.repositories

import com.example.rental_property_management_app.models.LandlordDashboardResponse
import com.example.rental_property_management_app.network.RetrofitClient
import com.example.rental_property_management_app.utils.Resource

class DashboardRepository {

    private val apiService = RetrofitClient.apiService

    suspend fun getLandlordDashboard(): Resource<LandlordDashboardResponse> =
        ApiResultHandler.execute {
            apiService.getLandlordDashboard()
        }
}
