package com.example.rental_property_management_app.repositories

import com.example.rental_property_management_app.network.RetrofitClient
import com.example.rental_property_management_app.utils.Constants

class TenantPortalRepository {
    private val api = RetrofitClient.apiService

    suspend fun dashboard() = apiResult({ api.tenantDashboard() }) { response ->
        response.data?.let { dashboard ->
            if (Constants.USE_MOCK_TENANT_DATA) MockData.fillDashboard(dashboard) else dashboard
        }
    }
}
