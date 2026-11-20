package com.example.rental_property_management_app.models

import java.math.BigDecimal

data class TenantDashboard(
    val fullName: String,
    val roomCode: String?,
    val amountDue: BigDecimal?,
    val dueDate: String?,
    val unreadCount: Long
)

data class MyRoom(
    val roomCode: String?,
    val address: String?,
    val rentPrice: BigDecimal?,
    val amenities: List<String>,
    val landlordName: String?,
    val landlordPhone: String?,
    val healthScore: Int? = null,
    val roomId: Long? = null,
    val propertyId: Long? = null,
    val floor: Int? = null,
    val area: BigDecimal? = null,
    val deposit: BigDecimal? = null,
    val electricityPrice: BigDecimal? = null,
    val waterPrice: BigDecimal? = null,
    val internetFee: BigDecimal? = null,
    val serviceFee: BigDecimal? = null,
    val capacity: Int? = null,
    val currentOccupants: Int? = null,
    val roomStatus: String? = null,
    val description: String? = null,
    val contractId: Long? = null,
    val contractCode: String? = null,
    val contractStatus: String? = null,
    val contractStartDate: String? = null,
    val contractEndDate: String? = null,
    val paymentDayOfMonth: Int? = null,
    val primaryTenantName: String? = null,
    val roommates: List<String> = emptyList(),
    val latestInvoiceMonth: String? = null,
    val latestInvoiceAmount: BigDecimal? = null,
    val latestInvoiceStatus: String? = null
)

data class MyContract(
    val id: Long?,
    val status: String?,
    val startDate: String?,
    val endDate: String?,
    val rentPrice: BigDecimal?,
    val deposit: BigDecimal?,
    val terms: String?,
    val contractCode: String? = null,
    val roomCode: String? = null,
    val propertyName: String? = null,
    val propertyAddress: String? = null,
    val landlordName: String? = null,
    val landlordPhone: String? = null,
    val electricityUnitPrice: BigDecimal? = null,
    val waterUnitPrice: BigDecimal? = null,
    val waterBillingMode: String? = null,
    val internetFee: BigDecimal? = null,
    val garbageFee: BigDecimal? = null,
    val serviceFee: BigDecimal? = null,
    val paymentDayOfMonth: Int? = null,
    val documentUrl: String? = null,
    val signedAt: String? = null,
    val roommates: List<RoommateResponse> = emptyList()
)

enum class TenantInvoiceStatus { UNPAID, PARTIALLY_PAID, PAID, OVERDUE }

data class TenantInvoice(
    val id: Long,
    val invoiceCode: String,
    val billingMonth: String,
    val status: TenantInvoiceStatus,
    val dueDate: String?,
    val roomRent: BigDecimal,
    val electricityAmount: BigDecimal,
    val waterAmount: BigDecimal,
    val internetAmount: BigDecimal,
    val serviceAmount: BigDecimal,
    val otherFees: BigDecimal,
    val previousDebt: BigDecimal,
    val discountAmount: BigDecimal,
    val totalAmount: BigDecimal,
    val paidAmount: BigDecimal,
    val remainingAmount: BigDecimal,
    val propertyName: String? = null,
    val roomCode: String? = null,
    val tenantName: String? = null,
    val contractCode: String? = null,
    val prevElectricIndex: BigDecimal? = null,
    val currElectricIndex: BigDecimal? = null,
    val electricUsage: BigDecimal? = null,
    val electricUnitPrice: BigDecimal? = null,
    val prevWaterIndex: BigDecimal? = null,
    val currWaterIndex: BigDecimal? = null,
    val waterUsage: BigDecimal? = null,
    val waterUnitPrice: BigDecimal? = null,
    val waterBillingMode: String? = null,
    val occupantCount: Int? = null,
    val garbageFee: BigDecimal? = null,
    val adjustmentAmount: BigDecimal? = null,
    val adjustmentReason: String? = null,
    val notes: String? = null,
    val publishedAt: String? = null,
    val paymentProofNote: String? = null,
    val paymentProofUrl: String? = null,
    val paymentProofSubmittedAt: String? = null,
    val payments: List<TenantPayment> = emptyList()
)

data class TenantPaymentInfo(
    val bankName: String,
    val accountNumber: String,
    val amount: BigDecimal,
    val transferContent: String,
    val qrPayload: String,
    val isSample: Boolean = false
)

data class TenantPayment(
    val id: Long,
    val invoiceId: Long,
    val paidAt: String?,
    val amount: BigDecimal,
    val method: String,
    val status: String
)

data class SubmitPaymentProofRequest(
    val note: String? = null,
    val proofUrl: String? = null
)
