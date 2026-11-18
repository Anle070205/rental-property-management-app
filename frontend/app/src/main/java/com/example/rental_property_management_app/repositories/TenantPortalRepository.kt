package com.example.rental_property_management_app.repositories

import com.example.rental_property_management_app.models.ApiResponse
import com.example.rental_property_management_app.models.TenantInvoiceStatus
import com.example.rental_property_management_app.network.RetrofitClient
import retrofit2.Response
import com.example.rental_property_management_app.utils.Constants

class TenantPortalRepository {
    private val api = RetrofitClient.apiService

    suspend fun invoices(month: String?, status: TenantInvoiceStatus?, page: Int) =
        apiResult({ api.tenantInvoices(month, status, page) }) { response ->
            response.data?.let {
                if (Constants.USE_MOCK_TENANT_DATA && it.totalElements == 0L) MockData.invoicePage(month, status, it)
                else it
            }
        }

    suspend fun invoice(id: Long) = apiResult({
        val response = api.tenantInvoice(id)
        val sample = if (Constants.USE_MOCK_TENANT_DATA && response.code() == 404) MockData.invoice(id) else null
        if (sample == null) response else {
            response.errorBody()?.close()
            Response.success(ApiResponse(true, "", sample))
        }
    }) { it.data }

    suspend fun contract() = apiResult({ api.myContract() }) { response ->
        response.data?.let { if (Constants.USE_MOCK_TENANT_DATA && it.id == null) MockData.contract else it }
    }

    suspend fun requestRenewal(id: Long) = apiResult({ api.requestContractRenewal(id) }) { Unit }

    suspend fun room() = apiResult({ api.myRoom() }) { response ->
        response.data?.let { if (Constants.USE_MOCK_TENANT_DATA && it.roomCode == null) MockData.room else it }
    }

    suspend fun dashboard() = apiResult({ api.tenantDashboard() }) { response ->
        response.data?.let { dashboard ->
            if (Constants.USE_MOCK_TENANT_DATA) MockData.fillDashboard(dashboard) else dashboard
        }
    }
}
