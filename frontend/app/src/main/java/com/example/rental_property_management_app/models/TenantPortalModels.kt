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
    val roomCode: String?, val address: String?, val rentPrice: BigDecimal?,
    val amenities: List<String>, val landlordName: String?, val landlordPhone: String?,
    val healthScore: Int? = null
)

data class MyContract(
    val id: Long?, val status: String?, val startDate: String?, val endDate: String?,
    val rentPrice: BigDecimal?, val deposit: BigDecimal?, val terms: String?
)

enum class TenantInvoiceStatus { UNPAID, PARTIALLY_PAID, PAID, OVERDUE }

data class TenantInvoice(
    val id: Long, val invoiceCode: String, val billingMonth: String, val status: TenantInvoiceStatus,
    val dueDate: String?, val roomRent: BigDecimal, val electricityAmount: BigDecimal, val waterAmount: BigDecimal,
    val internetAmount: BigDecimal, val serviceAmount: BigDecimal, val otherFees: BigDecimal,
    val previousDebt: BigDecimal, val discountAmount: BigDecimal, val totalAmount: BigDecimal,
    val paidAmount: BigDecimal, val remainingAmount: BigDecimal
)

data class TenantPaymentInfo(
    val bankName: String, val accountNumber: String, val amount: BigDecimal,
    val transferContent: String, val qrPayload: String, val isSample: Boolean = false
)

data class TenantPayment(
    val id: Long, val invoiceId: Long, val paidAt: String?, val amount: BigDecimal,
    val method: String, val status: String
)
