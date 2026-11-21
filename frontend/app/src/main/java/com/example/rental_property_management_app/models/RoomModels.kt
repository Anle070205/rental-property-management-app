package com.example.rental_property_management_app.models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

/**
 * Room Status Enum - Khớp với Backend RoomStatus
 */
enum class RoomStatus {
    @SerializedName("VACANT")
    VACANT,           // Còn trống (AVAILABLE)
    
    @SerializedName("OCCUPIED")
    OCCUPIED,         // Đang thuê (RENTED)
    
    @SerializedName("EXPIRING_SOON")
    EXPIRING_SOON,    // Sắp hết hạn (EXPIRING)
    
    @SerializedName("NEEDS_ATTENTION")
    NEEDS_ATTENTION;  // Cần chú ý (ATTENTION)
    
    fun getDisplayName(): String {
        return when (this) {
            VACANT -> "Còn trống"
            OCCUPIED -> "Đang thuê"
            EXPIRING_SOON -> "Sắp hết hạn"
            NEEDS_ATTENTION -> "Cần chú ý"
        }
    }
    
    fun getColorResId(): Int {
        return when (this) {
            VACANT -> android.R.color.holo_green_dark
            OCCUPIED -> android.R.color.holo_blue_dark
            EXPIRING_SOON -> android.R.color.holo_orange_dark
            NEEDS_ATTENTION -> android.R.color.holo_red_dark
        }
    }
}

/**
 * Room Response từ Backend
 */
data class RoomResponse(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("propertyId")
    val propertyId: Long,
    
    @SerializedName("propertyName")
    val propertyName: String,

    @SerializedName("propertyAddress")
    val propertyAddress: String? = null,

    @SerializedName("landlordName")
    val landlordName: String? = null,

    @SerializedName("landlordPhone")
    val landlordPhone: String? = null,
    
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
    val electricityPrice: BigDecimal? = null,

    @SerializedName("waterPrice")
    val waterPrice: BigDecimal? = null,

    @SerializedName("internetFee")
    val internetFee: BigDecimal? = null,

    @SerializedName("serviceFee")
    val serviceFee: BigDecimal? = null,

    @SerializedName("healthScore")
    val healthScore: Int = 100,
    
    @SerializedName("amenities")
    val amenities: String?,
    
    @SerializedName("description")
    val description: String?,
    
    @SerializedName("imageUrl")
    val imageUrl: String?,
    
    @SerializedName("status")
    val status: RoomStatus,
    
    @SerializedName("currentContract")
    val currentContract: String?,
    
    @SerializedName("currentTenant")
    val currentTenant: String?,

    @SerializedName("currentContractId")
    val currentContractId: Long? = null,

    @SerializedName("currentContractStatus")
    val currentContractStatus: String? = null,

    @SerializedName("currentTenantId")
    val currentTenantId: Long? = null,

    @SerializedName("currentOccupants")
    val currentOccupants: Int = 0,

    @SerializedName("currentRoommates")
    val currentRoommates: List<String> = emptyList(),

    @SerializedName("canAddPrimaryTenant")
    val canAddPrimaryTenant: Boolean = true,

    @SerializedName("canAddRoommate")
    val canAddRoommate: Boolean = false,

    @SerializedName("occupancyMessage")
    val occupancyMessage: String? = null,

    @SerializedName("createdAt")
    val createdAt: String,
    
    @SerializedName("updatedAt")
    val updatedAt: String
)

/**
 * Create Room Request
 */
data class CreateRoomRequest(
    @SerializedName("propertyId")
    val propertyId: Long,
    
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
    
    @SerializedName("amenities")
    val amenities: String?,
    
    @SerializedName("description")
    val description: String?,
    
    @SerializedName("imageUrl")
    val imageUrl: String?,

    @SerializedName("electricityPrice")
    val electricityPrice: BigDecimal? = null,

    @SerializedName("waterPrice")
    val waterPrice: BigDecimal? = null,

    @SerializedName("internetFee")
    val internetFee: BigDecimal? = null,

    @SerializedName("serviceFee")
    val serviceFee: BigDecimal? = null
)

/**
 * Update Room Request
 */
data class UpdateRoomRequest(
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
    
    @SerializedName("amenities")
    val amenities: String?,
    
    @SerializedName("description")
    val description: String?,
    
    @SerializedName("imageUrl")
    val imageUrl: String?,
    
    @SerializedName("status")
    val status: RoomStatus,

    @SerializedName("electricityPrice")
    val electricityPrice: BigDecimal? = null,

    @SerializedName("waterPrice")
    val waterPrice: BigDecimal? = null,

    @SerializedName("internetFee")
    val internetFee: BigDecimal? = null,

    @SerializedName("serviceFee")
    val serviceFee: BigDecimal? = null
)
