package com.example.rental_property_management_app.repositories

import com.example.rental_property_management_app.models.DebtSummaryResponse
import com.example.rental_property_management_app.models.RoomTenantDebtResponse
import com.example.rental_property_management_app.network.RetrofitClient
import com.example.rental_property_management_app.utils.Resource

class DebtRepository {

    private val apiService = RetrofitClient.apiService

    suspend fun getDebts(
        roomId: Long? = null,
        tenantId: Long? = null,
        month: String? = null,
        overdueLevel: String? = null,
        keyword: String? = null
    ): Resource<DebtSummaryResponse> = ApiResultHandler.execute {
        apiService.getDebts(roomId, tenantId, month, overdueLevel, keyword)
    }

    suspend fun getTenantDebtDetail(tenantId: Long): Resource<RoomTenantDebtResponse> =
        ApiResultHandler.execute {
            apiService.getTenantDebtDetail(tenantId)
        }
}
