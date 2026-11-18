package com.example.rental_property_management_app.models

import com.example.rental_property_management_app.R
import com.google.gson.annotations.SerializedName
import java.math.BigDecimal

enum class InvoiceStatus {
    @SerializedName("UNPAID")
    UNPAID,

    @SerializedName("PARTIALLY_PAID")
    PARTIALLY_PAID,

    @SerializedName("PAID")
    PAID,

    @SerializedName("OVERDUE")
    OVERDUE;

    fun getDisplayName(): String {
        return when (this) {
            UNPAID -> "Chưa thanh toán"
            PARTIALLY_PAID -> "Thanh toán 1 phần"
            PAID -> "Đã thanh toán"
            OVERDUE -> "Quá hạn"
        }
    }

    fun getTextColorRes(): Int {
        return when (this) {
            UNPAID -> R.color.invoice_unpaid
            PARTIALLY_PAID -> R.color.invoice_partial
            PAID -> R.color.invoice_paid
            OVERDUE -> R.color.invoice_overdue
        }
    }

    fun getBgColorRes(): Int {
        return when (this) {
            UNPAID -> R.color.invoice_unpaid_bg
            PARTIALLY_PAID -> R.color.invoice_partial_bg
            PAID -> R.color.invoice_paid_bg
            OVERDUE -> R.color.invoice_overdue_bg
        }
    }
}

enum class PaymentMethod {
    @SerializedName("CASH")
    CASH,

    @SerializedName("BANK_TRANSFER")
    BANK_TRANSFER;

    fun getDisplayName(): String {
        return when (this) {
            CASH -> "Tiền mặt"
            BANK_TRANSFER -> "Chuyển khoản"
        }
    }
}

data class PaymentResponse(
    @SerializedName("id")
    val id: Long,

    @SerializedName("invoiceId")
    val invoiceId: Long,

    @SerializedName("amount")
    val amount: BigDecimal,

    @SerializedName("paymentMethod")
    val paymentMethod: String,

    @SerializedName("transactionCode")
    val transactionCode: String?,

    @SerializedName("receiptNote")
    val receiptNote: String?,

    @SerializedName("paidAt")
    val paidAt: String,

    @SerializedName("createdAt")
    val createdAt: String
)

data class InvoiceResponse(
    @SerializedName("id")
    val id: Long,

    @SerializedName("invoiceCode")
    val invoiceCode: String,

    @SerializedName("propertyId")
    val propertyId: Long,

    @SerializedName("propertyName")
    val propertyName: String,

    @SerializedName("roomId")
    val roomId: Long,

    @SerializedName("roomCode")
    val roomCode: String,

    @SerializedName("tenantId")
    val tenantId: Long?,

    @SerializedName("tenantName")
    val tenantName: String?,

    @SerializedName("tenantPhone")
    val tenantPhone: String?,

    @SerializedName("utilityRecordId")
    val utilityRecordId: Long?,

    @SerializedName("billingMonth")
    val billingMonth: String,

    @SerializedName("roomRent")
    val roomRent: BigDecimal,

    @SerializedName("electricityAmount")
    val electricityAmount: BigDecimal,

    @SerializedName("waterAmount")
    val waterAmount: BigDecimal,

    @SerializedName("internetAmount")
    val internetAmount: BigDecimal,

    @SerializedName("serviceAmount")
    val serviceAmount: BigDecimal,

    @SerializedName("otherFees")
    val otherFees: BigDecimal,

    @SerializedName("previousDebt")
    val previousDebt: BigDecimal,

    @SerializedName("discountAmount")
    val discountAmount: BigDecimal,

    @SerializedName("totalAmount")
    val totalAmount: BigDecimal,

    @SerializedName("paidAmount")
    val paidAmount: BigDecimal,

    @SerializedName("remainingAmount")
    val remainingAmount: BigDecimal,

    @SerializedName("status")
    val status: InvoiceStatus,

    @SerializedName("dueDate")
    val dueDate: String,

    @SerializedName("daysOverdue")
    val daysOverdue: Long,

    @SerializedName("notes")
    val notes: String?,

    @SerializedName("electricUsage")
    val electricUsage: BigDecimal?,

    @SerializedName("waterUsage")
    val waterUsage: BigDecimal?,

    @SerializedName("payments")
    val payments: List<PaymentResponse> = emptyList(),

    @SerializedName("createdAt")
    val createdAt: String?,

    @SerializedName("updatedAt")
    val updatedAt: String?
)

data class CreateInvoiceRequest(
    @SerializedName("roomId")
    val roomId: Long,

    @SerializedName("tenantId")
    val tenantId: Long? = null,

    @SerializedName("billingMonth")
    val billingMonth: String,

    @SerializedName("dueDate")
    val dueDate: String,

    @SerializedName("roomRent")
    val roomRent: BigDecimal? = null,

    @SerializedName("electricityAmount")
    val electricityAmount: BigDecimal? = null,

    @SerializedName("waterAmount")
    val waterAmount: BigDecimal? = null,

    @SerializedName("internetAmount")
    val internetAmount: BigDecimal? = null,

    @SerializedName("serviceAmount")
    val serviceAmount: BigDecimal? = null,

    @SerializedName("otherFees")
    val otherFees: BigDecimal = BigDecimal.ZERO,

    @SerializedName("previousDebt")
    val previousDebt: BigDecimal? = null,

    @SerializedName("discountAmount")
    val discountAmount: BigDecimal = BigDecimal.ZERO,

    @SerializedName("useUtilityRecord")
    val useUtilityRecord: Boolean = true,

    @SerializedName("notes")
    val notes: String? = null
)

data class BatchCreateInvoiceRequest(
    @SerializedName("propertyId")
    val propertyId: Long? = null,

    @SerializedName("roomIds")
    val roomIds: List<Long>? = null,

    @SerializedName("billingMonth")
    val billingMonth: String,

    @SerializedName("dueDate")
    val dueDate: String,

    @SerializedName("defaultInternetFee")
    val defaultInternetFee: BigDecimal = BigDecimal.ZERO,

    @SerializedName("defaultServiceFee")
    val defaultServiceFee: BigDecimal = BigDecimal.ZERO,

    @SerializedName("otherFees")
    val otherFees: BigDecimal = BigDecimal.ZERO,

    @SerializedName("onlyOccupiedRooms")
    val onlyOccupiedRooms: Boolean = true,

    @SerializedName("notes")
    val notes: String? = null
)

data class BatchCreateInvoiceResult(
    @SerializedName("createdCount")
    val createdCount: Int,

    @SerializedName("skippedCount")
    val skippedCount: Int,

    @SerializedName("createdInvoices")
    val createdInvoices: List<InvoiceResponse>,

    @SerializedName("skippedReasons")
    val skippedReasons: List<String>
)

data class CreatePaymentRequest(
    @SerializedName("amount")
    val amount: BigDecimal,

    @SerializedName("paymentMethod")
    val paymentMethod: PaymentMethod,

    @SerializedName("transactionCode")
    val transactionCode: String? = null,

    @SerializedName("receiptNote")
    val receiptNote: String? = null,

    @SerializedName("confirmedByLandlord")
    val confirmedByLandlord: Boolean = true
)
