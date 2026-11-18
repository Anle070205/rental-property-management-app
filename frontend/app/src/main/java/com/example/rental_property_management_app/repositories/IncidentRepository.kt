package com.example.rental_property_management_app.repositories

import com.example.rental_property_management_app.models.*
import com.example.rental_property_management_app.network.RetrofitClient

class IncidentRepository {
    private val api = RetrofitClient.apiService
    suspend fun list(status: IncidentStatus?, page: Int) = apiResult({ api.incidents(status, page) }) { it.data }
    suspend fun get(id: Long) = apiResult({ api.incident(id) }) { it.data }
    suspend fun create(request: CreateIncidentRequest) = apiResult({ api.createIncident(request) }) { it.data }
    suspend fun supplement(id: Long, request: UpdateIncidentRequest) =
        apiResult({ api.supplementIncident(id, request) }) { it.data }
}
