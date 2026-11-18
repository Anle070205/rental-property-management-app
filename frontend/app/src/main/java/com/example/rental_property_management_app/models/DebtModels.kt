package com.example.rental_property_management_app.models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class DebtHistoryItemResponse(
    @SerializedName("type")
    val type: String,

    @SerializedName("referenceCode")
    val referenceCode: String,

    @SerializedName("billingMonth")
    val billingMonth: String,

    @SerializedName("amount")
    val amount: BigDecimal,

    @SerializedName("balanceAfter")
    val balanceAfter: BigDecimal,

    @SerializedName("occurredDate")
    val occurredDate: String,

    @SerializedName("description")
    val description: String
)

data class RoomTenantDebtResponse(
    @SerializedName("roomId")
    val roomId: Long?,

    @SerializedName("roomCode")
    val roomCode: String?,

    @SerializedName("propertyId")
    val propertyId: Long?,

    @SerializedName("propertyName")
    val propertyName: String?,

    @SerializedName("tenantId")
    val tenantId: Long?,

    @SerializedName("tenantName")
    val tenantName: String?,

    @SerializedName("tenantPhone")
    val tenantPhone: String?,

    @SerializedName("totalBilled")
    val totalBilled: BigDecimal,

    @SerializedName("totalPaid")
    val totalPaid: BigDecimal,

    @SerializedName("totalDebt")
    val totalDebt: BigDecimal,

    @SerializedName("unpaidInvoicesCount")
    val unpaidInvoicesCount: Int,

    @SerializedName("overdueInvoicesCount")
    val overdueInvoicesCount: Int,

    @SerializedName("maxDaysOverdue")
    val maxDaysOverdue: Long,

    @SerializedName("overdueLevel")
    val overdueLevel: String,

    @SerializedName("latestInvoiceId")
    val latestInvoiceId: Long?,

    @SerializedName("unpaidInvoices")
    val unpaidInvoices: List<InvoiceResponse> = emptyList(),

    @SerializedName("debtHistory")
    val debtHistory: List<DebtHistoryItemResponse> = emptyList()
)

data class DebtSummaryResponse(
    @SerializedName("totalDebtAmount")
    val totalDebtAmount: BigDecimal,

    @SerializedName("totalUnpaidInvoices")
    val totalUnpaidInvoices: Int,

    @SerializedName("totalOverdueInvoices")
    val totalOverdueInvoices: Int,

    @SerializedName("roomsWithDebtCount")
    val roomsWithDebtCount: Int,

    @SerializedName("items")
    val items: List<RoomTenantDebtResponse> = emptyList()
)
