package com.example.rental_property_management_app.models

import com.google.gson.annotations.SerializedName

data class AdminDashboardResponse(
    val adminName: String = "",
    val totalAccounts: Long = 0,
    val landlordCount: Long = 0,
    val tenantCount: Long = 0,
    val adminCount: Long = 0,
    val propertyCount: Long = 0,
    val totalRooms: Long = 0,
    val occupiedRooms: Long = 0,
    val activeAccounts: Long = 0,
    val lockedAccounts: Long = 0,
    val activePercentage: Double = 0.0,
    val monthlyGrowth: List<AdminMonthlyPoint> = emptyList(),
    val recentActivities: List<AdminRecentActivity> = emptyList()
)

data class AdminMonthlyPoint(val label: String = "", val count: Long = 0)

data class AdminRecentActivity(
    val timeLabel: String = "",
    val title: String = "",
    val subtitle: String = "",
    val type: String = "",
    val refId: Long? = null
)

data class AdminAccountItem(
    val id: Long = 0,
    val accountCode: String = "",
    val fullName: String = "",
    val email: String? = null,
    val phone: String? = null,
    val role: String = "",
    @SerializedName("isActive")
    val isActive: Boolean = true,
    val emailVerified: Boolean = false,
    val phoneVerified: Boolean = false,
    val createdAt: String? = null,
    val landlordId: Long? = null,
    val landlordName: String? = null,
    val landlordCode: String? = null,
    val propertyId: Long? = null,
    val propertyName: String? = null,
    val roomId: Long? = null,
    val roomCode: String? = null,
    val hasTenantLink: Boolean = false,
    val noteMessage: String? = null
)

data class AdminAccountPageResult(
    val items: List<AdminAccountItem> = emptyList(),
    val page: Int = 0,
    val size: Int = 10,
    val totalElements: Long = 0,
    val totalPages: Int = 1,
    val allCount: Long = 0,
    val adminCount: Long = 0,
    val landlordCount: Long = 0,
    val tenantCount: Long = 0,
    val activeCount: Long = 0,
    val lockedCount: Long = 0
)

data class AdminCreateAccountRequest(
    val fullName: String,
    val email: String,
    val phone: String? = null,
    val role: String,
    val isActive: Boolean = true,
    val password: String? = null,
    val landlordId: Long? = null,
    val propertyId: Long? = null,
    val roomId: Long? = null
)

data class AdminUpdateAccountRequest(
    val fullName: String,
    val email: String,
    val phone: String? = null,
    val role: String? = null,
    val isActive: Boolean? = null
)

data class AdminLandlordItem(
    val id: Long = 0,
    val landlordCode: String = "",
    val fullName: String = "",
    val email: String? = null,
    val phone: String? = null,
    @SerializedName("isActive")
    val isActive: Boolean = true,
    val createdAt: String? = null,
    val address: String? = null,
    val propertyCount: Long = 0,
    val roomCount: Long = 0,
    val tenantCount: Long = 0,
    val properties: List<AdminLandlordPropertySummary> = emptyList(),
    val tenants: List<AdminLandlordTenantSummary> = emptyList()
)

data class AdminLandlordPropertySummary(
    val id: Long = 0,
    val name: String = "",
    val roomCount: Long = 0,
    val occupiedCount: Long = 0,
    val tenantCount: Long = 0
)

data class AdminLandlordTenantSummary(
    val tenantId: Long = 0,
    val userId: Long? = null,
    val fullName: String = "",
    val propertyName: String = "",
    val roomCode: String = ""
)

data class AdminLandlordListResult(
    val items: List<AdminLandlordItem> = emptyList(),
    val totalCount: Long = 0,
    val activeCount: Long = 0,
    val lockedCount: Long = 0
)

data class AdminPropertyItem(
    val id: Long = 0,
    val propertyCode: String = "",
    val name: String = "",
    val address: String? = null,
    val landlordId: Long? = null,
    val landlordName: String = "",
    val landlordCode: String = "",
    val statusLabel: String = "",
    val totalRooms: Long = 0,
    val occupiedRooms: Long = 0,
    val vacantRooms: Long = 0,
    val maintenanceRooms: Long = 0,
    val tenantCount: Long = 0,
    val occupancyPercent: Int = 0,
    val occupiedRoomLookups: List<AdminOccupiedRoomLookup> = emptyList()
)

data class AdminOccupiedRoomLookup(
    val roomId: Long = 0,
    val roomCode: String = "",
    val floor: Int? = null,
    val tenantUserId: Long? = null,
    val tenantName: String = ""
)

data class AdminPropertyListResult(
    val items: List<AdminPropertyItem> = emptyList(),
    val totalProperties: Long = 0,
    val totalRooms: Long = 0,
    val activeProperties: Long = 0,
    val pausedProperties: Long = 0
)

data class AdminReportItem(
    val id: Long = 0,
    val reportCode: String = "",
    val title: String = "",
    val description: String = "",
    val type: String = "",
    val priority: String = "",
    val status: String = "",
    val senderUserId: Long? = null,
    val senderName: String = "",
    val senderRole: String = "",
    val landlordName: String = "",
    val propertyName: String = "",
    val roomCode: String = "",
    val resolutionNote: String? = null,
    val resolvedByName: String? = null,
    val resolvedAt: String? = null,
    val createdAt: String? = null
)

data class AdminReportListResult(
    val items: List<AdminReportItem> = emptyList(),
    val totalCount: Long = 0,
    val pendingCount: Long = 0,
    val inProgressCount: Long = 0,
    val doneCount: Long = 0
)

data class AdminResolveReportRequest(val responseNote: String, val status: String)

data class AdminBroadcastItem(
    val id: Long = 0,
    val broadcastCode: String = "",
    val title: String = "",
    val content: String = "",
    val targetAudience: String = "ALL",
    val status: String = "DRAFT",
    val recipientCount: Int = 0,
    val createdByName: String? = null,
    val sentAt: String? = null,
    val updatedAt: String? = null
)

data class AdminBroadcastAudienceCounts(
    val allActive: Long = 0,
    val adminActive: Long = 0,
    val landlordActive: Long = 0,
    val tenantActive: Long = 0,
    val lockedCount: Long = 0
)

data class AdminBroadcastListResult(
    val items: List<AdminBroadcastItem> = emptyList(),
    val totalCount: Long = 0,
    val sentCount: Long = 0,
    val draftCount: Long = 0
)

data class AdminBroadcastRequest(val title: String, val content: String, val targetAudience: String)

data class AdminChangePasswordResponse(val accessToken: String = "", val message: String = "")
