package com.example.rental_property_management_app.models

data class GuestStayResponse(
    val id: Long,
    val tenantId: Long? = null,
    val tenantName: String? = null,
    val roomId: Long? = null,
    val roomCode: String? = null,
    val propertyName: String? = null,
    val guestFullName: String,
    val maskedIdentityNumber: String,
    val guestDateOfBirth: String? = null,
    val guestGender: String? = null,
    val guestPhone: String? = null,
    val relationship: String,
    val stayStartAt: String,
    val stayEndAt: String,
    val note: String? = null,
    val consentConfirmed: Boolean = false,
    val qrVerified: Boolean = false,
    val status: String,
    val landlordNote: String? = null,
    val retentionUntil: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
    val legalDisclaimer: String? = null
)

data class CreateGuestStayRequest(
    val guestFullName: String,
    val guestIdentityNumber: String,
    val guestDateOfBirth: String? = null,
    val guestGender: String? = null,
    val guestPhone: String? = null,
    val relationship: String,
    val stayStartAt: String,
    val stayEndAt: String,
    val note: String? = null,
    val consentConfirmed: Boolean,
    val qrVerified: Boolean
)

data class UpdateGuestStayStatusRequest(
    val status: String,
    val landlordNote: String? = null
)

fun guestStayStatusLabel(status: String): String = when (status.uppercase()) {
    "PENDING" -> "Chờ xử lý"
    "RECEIVED" -> "Đã tiếp nhận"
    "NEEDS_INFO" -> "Cần bổ sung"
    "COMPLETED" -> "Hoàn tất"
    "CANCELLED" -> "Đã hủy"
    else -> status
}

fun guestStayRelationshipLabel(relationship: String): String = when (relationship.uppercase()) {
    "FRIEND" -> "Bạn bè"
    "RELATIVE" -> "Người thân"
    "OTHER" -> "Khác"
    else -> relationship
}
