package com.example.rental_property_management_app.repositories

import com.example.rental_property_management_app.models.*
import com.example.rental_property_management_app.network.RetrofitClient
import com.example.rental_property_management_app.utils.Resource

class InvoiceRepository {

    private val apiService = RetrofitClient.apiService

    suspend fun getInvoices(
        month: String? = null,
        roomId: Long? = null,
        propertyId: Long? = null,
        status: InvoiceStatus? = null
    ): Resource<List<InvoiceResponse>> = ApiResultHandler.execute {
        apiService.getInvoices(month, roomId, propertyId, status?.name)
    }

    suspend fun createInvoice(request: CreateInvoiceRequest): Resource<InvoiceResponse> =
        ApiResultHandler.execute {
            apiService.createInvoice(request)
        }

    suspend fun batchCreateInvoices(request: BatchCreateInvoiceRequest): Resource<BatchCreateInvoiceResult> =
        ApiResultHandler.execute {
            apiService.batchCreateInvoices(request)
        }

    suspend fun getInvoiceById(id: Long): Resource<InvoiceResponse> =
        ApiResultHandler.execute {
            apiService.getInvoiceById(id)
        }

    suspend fun recordPayment(
        invoiceId: Long,
        request: CreatePaymentRequest
    ): Resource<InvoiceResponse> = ApiResultHandler.execute {
        apiService.recordPayment(invoiceId, request)
    }
}
