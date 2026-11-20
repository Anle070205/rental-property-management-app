package com.example.rental_property_management_app.models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class MonthlyRevenueResponse(
    @SerializedName("billingMonth")
    val billingMonth: String,

    @SerializedName("label")
    val label: String,

    @SerializedName("totalBilled")
    val totalBilled: BigDecimal,

    @SerializedName("totalCollected")
    val totalCollected: BigDecimal,

    @SerializedName("totalUnpaid")
    val totalUnpaid: BigDecimal
)

data class AttentionItemResponse(
    @SerializedName("roomId")
    val roomId: Long,

    @SerializedName("roomCode")
    val roomCode: String,

    @SerializedName("propertyName")
    val propertyName: String,

    @SerializedName("tenantName")
    val tenantName: String?,

    @SerializedName("category")
    val category: String,

    @SerializedName("reason")
    val reason: String,

    @SerializedName("healthScore")
    val healthScore: Int,

    @SerializedName("unpaidDebt")
    val unpaidDebt: BigDecimal
)

data class LandlordDashboardResponse(
    @SerializedName("totalProperties")
    val totalProperties: Int,

    @SerializedName("totalRooms")
    val totalRooms: Int,

    @SerializedName("occupiedRooms")
    val occupiedRooms: Int,

    @SerializedName("vacantRooms")
    val vacantRooms: Int,

    @SerializedName("expiringSoonRooms")
    val expiringSoonRooms: Int,

    @SerializedName("issueRooms")
    val issueRooms: Int,

    @SerializedName("totalRevenue")
    val totalRevenue: BigDecimal,

    @SerializedName("collectedAmount")
    val collectedAmount: BigDecimal,

    @SerializedName("uncollectedAmount")
    val uncollectedAmount: BigDecimal,

    @SerializedName("collectionRate")
    val collectionRate: Double,

    @SerializedName("averageHealthScore")
    val averageHealthScore: Int,

    @SerializedName("pendingBookingCount")
    val pendingBookingCount: Int = 0,

    @SerializedName("monthlyRevenues")
    val monthlyRevenues: List<MonthlyRevenueResponse> = emptyList(),

    @SerializedName("expiringContracts")
    val expiringContracts: List<AttentionItemResponse> = emptyList(),

    @SerializedName("attentionRooms")
    val attentionRooms: List<AttentionItemResponse> = emptyList()
)
