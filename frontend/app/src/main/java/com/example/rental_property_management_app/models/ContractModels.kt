package com.example.rental_property_management_app.models

import com.example.rental_property_management_app.R
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

enum class ContractStatus {
    @SerializedName("DRAFT")
    DRAFT,

    @SerializedName("SIGNED")
    SIGNED,

    @SerializedName("ACTIVE")
    ACTIVE,

    @SerializedName("EXPIRED")
    EXPIRED,

    @SerializedName("TERMINATED")
    TERMINATED;

    fun getDisplayName(): String = when (this) {
        DRAFT -> "Nháp"
        SIGNED -> "Đã ký (Chờ hiệu lực)"
        ACTIVE -> "Đang hiệu lực"
        EXPIRED -> "Hết hạn"
        TERMINATED -> "Đã thanh lý"
    }

    fun getTextColorRes(): Int = when (this) {
        DRAFT -> R.color.rc_muted
        SIGNED -> R.color.invoice_partial
        ACTIVE -> R.color.invoice_paid
        EXPIRED -> R.color.invoice_unpaid
        TERMINATED -> R.color.invoice_overdue
    }

    fun getBgColorRes(): Int = when (this) {
        DRAFT -> R.color.surface_variant
        SIGNED -> R.color.invoice_partial_bg
        ACTIVE -> R.color.invoice_paid_bg
        EXPIRED -> R.color.invoice_unpaid_bg
        TERMINATED -> R.color.invoice_overdue_bg
    }
}

enum class WaterBillingMode {
    @SerializedName("METERED")
    METERED,

    @SerializedName("FIXED")
    FIXED,

    @SerializedName("PER_PERSON")
    PER_PERSON;

    fun getDisplayName(): String = when (this) {
        METERED -> "Theo đồng hồ (m³)"
        FIXED -> "Khoán theo phòng"
        PER_PERSON -> "Theo đầu người"
    }
}

data class RoommateResponse(
    @SerializedName("id")
    val id: Long,

    @SerializedName("landlordId")
    val landlordId: Long,

    @SerializedName("tenantId")
    val tenantId: Long,

    @SerializedName("tenantName")
    val tenantName: String?,

    @SerializedName("roomId")
    val roomId: Long,

    @SerializedName("roomCode")
    val roomCode: String?,

    @SerializedName("contractId")
    val contractId: Long?,

    @SerializedName("contractCode")
    val contractCode: String?,

    @SerializedName("fullName")
    val fullName: String,

    @SerializedName("phone")
    val phone: String?,

    @SerializedName("identityNumber")
    val identityNumber: String?,

    @SerializedName("dateOfBirth")
    val dateOfBirth: String?,

    @SerializedName("relationship")
    val relationship: String?,

    @SerializedName("moveInDate")
    val moveInDate: String?,

    @SerializedName("isRepresentative")
    val isRepresentative: Boolean = false,

    @SerializedName("hasPortalAccess")
    val hasPortalAccess: Boolean = false,

    @SerializedName("consentConfirmed")
    val consentConfirmed: Boolean = false,

    @SerializedName("notes")
    val notes: String?,

    @SerializedName("createdAt")
    val createdAt: String? = null
)

data class CreateRoommateRequest(
    @SerializedName("tenantId")
    val tenantId: Long,

    @SerializedName("roomId")
    val roomId: Long,

    @SerializedName("contractId")
    val contractId: Long? = null,

    @SerializedName("fullName")
    val fullName: String,

    @SerializedName("phone")
    val phone: String? = null,

    @SerializedName("identityNumber")
    val identityNumber: String? = null,

    @SerializedName("dateOfBirth")
    val dateOfBirth: String? = null,

    @SerializedName("relationship")
    val relationship: String? = null,

    @SerializedName("moveInDate")
    val moveInDate: String? = null,

    @SerializedName("consentConfirmed")
    val consentConfirmed: Boolean = false,

    @SerializedName("notes")
    val notes: String? = null
)

data class UpdateRoommateRequest(
    @SerializedName("fullName")
    val fullName: String,

    @SerializedName("phone")
    val phone: String? = null,

    @SerializedName("identityNumber")
    val identityNumber: String? = null,

    @SerializedName("dateOfBirth")
    val dateOfBirth: String? = null,

    @SerializedName("relationship")
    val relationship: String? = null,

    @SerializedName("moveInDate")
    val moveInDate: String? = null,

    @SerializedName("consentConfirmed")
    val consentConfirmed: Boolean = false,

    @SerializedName("notes")
    val notes: String? = null
)

data class ContractResponse(
    @SerializedName("id")
    val id: Long,

    @SerializedName("contractCode")
    val contractCode: String,

    @SerializedName("landlordId")
    val landlordId: Long,

    @SerializedName("propertyId")
    val propertyId: Long,

    @SerializedName("propertyName")
    val propertyName: String,

    @SerializedName("roomId")
    val roomId: Long,

    @SerializedName("roomCode")
    val roomCode: String,

    @SerializedName("tenantId")
    val tenantId: Long,

    @SerializedName("tenantName")
    val tenantName: String,

    @SerializedName("tenantPhone")
    val tenantPhone: String?,

    @SerializedName("tenantEmail")
    val tenantEmail: String?,

    @SerializedName("tenantAccountStatus")
    val tenantAccountStatus: String? = null,

    @SerializedName("startDate")
    val startDate: String,

    @SerializedName("endDate")
    val endDate: String,

    @SerializedName("monthlyRent")
    val monthlyRent: BigDecimal,

    @SerializedName("depositAmount")
    val depositAmount: BigDecimal,

    @SerializedName("electricityUnitPrice")
    val electricityUnitPrice: BigDecimal,

    @SerializedName("waterUnitPrice")
    val waterUnitPrice: BigDecimal,

    @SerializedName("waterBillingMode")
    val waterBillingMode: WaterBillingMode = WaterBillingMode.METERED,

    @SerializedName("internetFee")
    val internetFee: BigDecimal = BigDecimal.ZERO,

    @SerializedName("garbageFee")
    val garbageFee: BigDecimal = BigDecimal.ZERO,

    @SerializedName("serviceFee")
    val serviceFee: BigDecimal = BigDecimal.ZERO,

    @SerializedName("paymentDayOfMonth")
    val paymentDayOfMonth: Int = 5,

    @SerializedName("terms")
    val terms: String?,

    @SerializedName("documentUrl")
    val documentUrl: String?,

    @SerializedName("status")
    val status: ContractStatus = ContractStatus.DRAFT,

    @SerializedName("signedAt")
    val signedAt: String?,

    @SerializedName("activatedAt")
    val activatedAt: String?,

    @SerializedName("terminatedAt")
    val terminatedAt: String?,

    @SerializedName("terminationReason")
    val terminationReason: String?,

    @SerializedName("roommates")
    val roommates: List<RoommateResponse> = emptyList(),

    @SerializedName("createdAt")
    val createdAt: String? = null,

    @SerializedName("updatedAt")
    val updatedAt: String? = null
)

data class CreateContractRequest(
    @SerializedName("roomId")
    val roomId: Long,

    @SerializedName("tenantId")
    val tenantId: Long,

    @SerializedName("startDate")
    val startDate: String,

    @SerializedName("endDate")
    val endDate: String,

    @SerializedName("monthlyRent")
    val monthlyRent: BigDecimal,

    @SerializedName("depositAmount")
    val depositAmount: BigDecimal = BigDecimal.ZERO,

    @SerializedName("electricityUnitPrice")
    val electricityUnitPrice: BigDecimal? = null,

    @SerializedName("waterUnitPrice")
    val waterUnitPrice: BigDecimal? = null,

    @SerializedName("waterBillingMode")
    val waterBillingMode: WaterBillingMode = WaterBillingMode.METERED,

    @SerializedName("internetFee")
    val internetFee: BigDecimal = BigDecimal.ZERO,

    @SerializedName("garbageFee")
    val garbageFee: BigDecimal = BigDecimal.ZERO,

    @SerializedName("serviceFee")
    val serviceFee: BigDecimal = BigDecimal.ZERO,

    @SerializedName("paymentDayOfMonth")
    val paymentDayOfMonth: Int = 5,

    @SerializedName("terms")
    val terms: String? = null,

    @SerializedName("documentUrl")
    val documentUrl: String? = null
)

data class UpdateContractRequest(
    @SerializedName("startDate")
    val startDate: String,

    @SerializedName("endDate")
    val endDate: String,

    @SerializedName("monthlyRent")
    val monthlyRent: BigDecimal,

    @SerializedName("depositAmount")
    val depositAmount: BigDecimal = BigDecimal.ZERO,

    @SerializedName("electricityUnitPrice")
    val electricityUnitPrice: BigDecimal? = null,

    @SerializedName("waterUnitPrice")
    val waterUnitPrice: BigDecimal? = null,

    @SerializedName("waterBillingMode")
    val waterBillingMode: WaterBillingMode = WaterBillingMode.METERED,

    @SerializedName("internetFee")
    val internetFee: BigDecimal = BigDecimal.ZERO,

    @SerializedName("garbageFee")
    val garbageFee: BigDecimal = BigDecimal.ZERO,

    @SerializedName("serviceFee")
    val serviceFee: BigDecimal = BigDecimal.ZERO,

    @SerializedName("paymentDayOfMonth")
    val paymentDayOfMonth: Int = 5,

    @SerializedName("terms")
    val terms: String? = null,

    @SerializedName("documentUrl")
    val documentUrl: String? = null
)

data class TerminateContractRequest(
    @SerializedName("reason")
    val reason: String? = null
)
