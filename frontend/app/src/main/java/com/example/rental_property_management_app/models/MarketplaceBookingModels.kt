package com.example.rental_property_management_app.models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

enum class BookingRequestType {
    @SerializedName("BOOKING")
    BOOKING,

    @SerializedName("CONTACT")
    CONTACT;

    fun getDisplayName(): String = when (this) {
        BOOKING -> "Đặt phòng"
        CONTACT -> "Liên hệ"
    }
}

enum class BookingRequestStatus {
    @SerializedName("PENDING")
    PENDING,

    @SerializedName("APPROVED")
    APPROVED,

    @SerializedName("REJECTED")
    REJECTED,

    @SerializedName("CANCELLED")
    CANCELLED;

    fun getDisplayName(): String = when (this) {
        PENDING -> "Chờ duyệt"
        APPROVED -> "Đã duyệt"
        REJECTED -> "Từ chối"
        CANCELLED -> "Đã hủy"
    }
}

data class MarketplaceProperty(
    @SerializedName("id")
    val id: Long,
    @SerializedName("name")
    val name: String,
    @SerializedName("address")
    val address: String,
    @SerializedName("description")
    val description: String?,
    @SerializedName("landlordId")
    val landlordId: Long,
    @SerializedName("landlordName")
    val landlordName: String,
    @SerializedName("landlordPhone")
    val landlordPhone: String?,
    @SerializedName("landlordEmail")
    val landlordEmail: String?,
    @SerializedName("totalRooms")
    val totalRooms: Int,
    @SerializedName("vacantRooms")
    val vacantRooms: Int,
    @SerializedName("minRentPrice")
    val minRentPrice: BigDecimal?,
    @SerializedName("maxRentPrice")
    val maxRentPrice: BigDecimal?,
    @SerializedName("averageHealthScore")
    val averageHealthScore: Int
)

data class MarketplaceRoom(
    @SerializedName("id")
    val id: Long,
    @SerializedName("propertyId")
    val propertyId: Long,
    @SerializedName("propertyName")
    val propertyName: String,
    @SerializedName("propertyAddress")
    val propertyAddress: String,
    @SerializedName("landlordId")
    val landlordId: Long,
    @SerializedName("landlordName")
    val landlordName: String,
    @SerializedName("landlordPhone")
    val landlordPhone: String?,
    @SerializedName("landlordEmail")
    val landlordEmail: String?,
    @SerializedName("roomCode")
    val roomCode: String,
    @SerializedName("floor")
    val floor: Int,
    @SerializedName("area")
    val area: BigDecimal,
    @SerializedName("rentPrice")
    val rentPrice: BigDecimal,
    @SerializedName("deposit")
    val deposit: BigDecimal,
    @SerializedName("capacity")
    val capacity: Int,
    @SerializedName("electricityPrice")
    val electricityPrice: BigDecimal?,
    @SerializedName("waterPrice")
    val waterPrice: BigDecimal?,
    @SerializedName("internetFee")
    val internetFee: BigDecimal?,
    @SerializedName("serviceFee")
    val serviceFee: BigDecimal?,
    @SerializedName("amenities")
    val amenities: String?,
    @SerializedName("description")
    val description: String?,
    @SerializedName("imageUrl")
    val imageUrl: String?,
    @SerializedName("status")
    val status: RoomStatus,
    @SerializedName("healthScore")
    val healthScore: Int,
    @SerializedName("healthRatingLabel")
    val healthRatingLabel: String,
    @SerializedName("hasPendingBookingByMe")
    val hasPendingBookingByMe: Boolean
)

data class CreateBookingRequest(
    @SerializedName("roomId")
    val roomId: Long,
    @SerializedName("requestType")
    val requestType: BookingRequestType,
    @SerializedName("moveInDate")
    val moveInDate: String? = null,
    @SerializedName("identityNumber")
    val identityNumber: String? = null,
    @SerializedName("message")
    val message: String? = null
)

data class RespondBookingRequest(
    @SerializedName("status")
    val status: BookingRequestStatus,
    @SerializedName("landlordNote")
    val landlordNote: String? = null
)

data class BookingRequestItem(
    @SerializedName("id")
    val id: Long,
    @SerializedName("roomId")
    val roomId: Long,
    @SerializedName("roomCode")
    val roomCode: String,
    @SerializedName("propertyId")
    val propertyId: Long,
    @SerializedName("propertyName")
    val propertyName: String,
    @SerializedName("propertyAddress")
    val propertyAddress: String?,
    @SerializedName("rentPrice")
    val rentPrice: BigDecimal?,
    @SerializedName("deposit")
    val deposit: BigDecimal?,
    @SerializedName("roomHealthScore")
    val roomHealthScore: Int,
    @SerializedName("landlordId")
    val landlordId: Long,
    @SerializedName("landlordName")
    val landlordName: String,
    @SerializedName("landlordPhone")
    val landlordPhone: String?,
    @SerializedName("tenantUserId")
    val tenantUserId: Long,
    @SerializedName("tenantName")
    val tenantName: String,
    @SerializedName("tenantPhone")
    val tenantPhone: String?,
    @SerializedName("tenantEmail")
    val tenantEmail: String?,
    @SerializedName("identityNumber")
    val identityNumber: String?,
    @SerializedName("requestType")
    val requestType: BookingRequestType,
    @SerializedName("status")
    val status: BookingRequestStatus,
    @SerializedName("moveInDate")
    val moveInDate: String?,
    @SerializedName("message")
    val message: String?,
    @SerializedName("landlordNote")
    val landlordNote: String?,
    @SerializedName("createdAt")
    val createdAt: String?,
    @SerializedName("updatedAt")
    val updatedAt: String?
)
