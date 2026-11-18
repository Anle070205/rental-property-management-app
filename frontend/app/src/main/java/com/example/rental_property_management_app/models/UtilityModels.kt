package com.example.rental_property_management_app.models

import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

data class UtilityRecordResponse(
    @SerializedName("id")
    val id: Long,

    @SerializedName("propertyId")
    val propertyId: Long,

    @SerializedName("propertyName")
    val propertyName: String,

    @SerializedName("roomId")
    val roomId: Long,

    @SerializedName("roomCode")
    val roomCode: String,

    @SerializedName("billingMonth")
    val billingMonth: String,

    @SerializedName("prevElectricIndex")
    val prevElectricIndex: BigDecimal,

    @SerializedName("currElectricIndex")
    val currElectricIndex: BigDecimal,

    @SerializedName("electricUsage")
    val electricUsage: BigDecimal,

    @SerializedName("electricUnitPrice")
    val electricUnitPrice: BigDecimal,

    @SerializedName("electricityAmount")
    val electricityAmount: BigDecimal,

    @SerializedName("prevWaterIndex")
    val prevWaterIndex: BigDecimal,

    @SerializedName("currWaterIndex")
    val currWaterIndex: BigDecimal,

    @SerializedName("waterUsage")
    val waterUsage: BigDecimal,

    @SerializedName("waterUnitPrice")
    val waterUnitPrice: BigDecimal,

    @SerializedName("waterAmount")
    val waterAmount: BigDecimal,

    @SerializedName("internetFee")
    val internetFee: BigDecimal,

    @SerializedName("serviceFee")
    val serviceFee: BigDecimal,

    @SerializedName("totalUtilityAmount")
    val totalUtilityAmount: BigDecimal,

    @SerializedName("isAdjustment")
    val isAdjustment: Boolean,

    @SerializedName("adjustmentReason")
    val adjustmentReason: String?,

    @SerializedName("notes")
    val notes: String?,

    @SerializedName("historyLog")
    val historyLog: String?,

    @SerializedName("linkedInvoiceId")
    val linkedInvoiceId: Long?,

    @SerializedName("linkedInvoiceCode")
    val linkedInvoiceCode: String?,

    @SerializedName("invoiceLocked")
    val invoiceLocked: Boolean,

    @SerializedName("createdAt")
    val createdAt: String?,

    @SerializedName("updatedAt")
    val updatedAt: String?
)

data class PreviousUtilityIndexResponse(
    @SerializedName("roomId")
    val roomId: Long,

    @SerializedName("previousBillingMonth")
    val previousBillingMonth: String?,

    @SerializedName("suggestedPrevElectricIndex")
    val suggestedPrevElectricIndex: BigDecimal,

    @SerializedName("suggestedPrevWaterIndex")
    val suggestedPrevWaterIndex: BigDecimal,

    @SerializedName("lastElectricUnitPrice")
    val lastElectricUnitPrice: BigDecimal,

    @SerializedName("lastWaterUnitPrice")
    val lastWaterUnitPrice: BigDecimal,

    @SerializedName("lastInternetFee")
    val lastInternetFee: BigDecimal,

    @SerializedName("lastServiceFee")
    val lastServiceFee: BigDecimal
)

data class CreateUtilityRequest(
    @SerializedName("roomId")
    val roomId: Long,

    @SerializedName("billingMonth")
    val billingMonth: String,

    @SerializedName("prevElectricIndex")
    val prevElectricIndex: BigDecimal,

    @SerializedName("currElectricIndex")
    val currElectricIndex: BigDecimal,

    @SerializedName("electricUnitPrice")
    val electricUnitPrice: BigDecimal,

    @SerializedName("prevWaterIndex")
    val prevWaterIndex: BigDecimal,

    @SerializedName("currWaterIndex")
    val currWaterIndex: BigDecimal,

    @SerializedName("waterUnitPrice")
    val waterUnitPrice: BigDecimal,

    @SerializedName("internetFee")
    val internetFee: BigDecimal,

    @SerializedName("serviceFee")
    val serviceFee: BigDecimal,

    @SerializedName("isAdjustment")
    val isAdjustment: Boolean = false,

    @SerializedName("adjustmentReason")
    val adjustmentReason: String? = null,

    @SerializedName("notes")
    val notes: String? = null
)

data class UpdateUtilityRequest(
    @SerializedName("prevElectricIndex")
    val prevElectricIndex: BigDecimal,

    @SerializedName("currElectricIndex")
    val currElectricIndex: BigDecimal,

    @SerializedName("electricUnitPrice")
    val electricUnitPrice: BigDecimal,

    @SerializedName("prevWaterIndex")
    val prevWaterIndex: BigDecimal,

    @SerializedName("currWaterIndex")
    val currWaterIndex: BigDecimal,

    @SerializedName("waterUnitPrice")
    val waterUnitPrice: BigDecimal,

    @SerializedName("internetFee")
    val internetFee: BigDecimal,

    @SerializedName("serviceFee")
    val serviceFee: BigDecimal,

    @SerializedName("isAdjustment")
    val isAdjustment: Boolean = false,

    @SerializedName("adjustmentReason")
    val adjustmentReason: String? = null,

    @SerializedName("notes")
    val notes: String? = null
)
