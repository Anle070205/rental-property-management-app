package com.example.rental_property_management_app.repositories

import com.example.rental_property_management_app.models.*
import java.math.BigDecimal

internal object MockData {
    val room = MyRoom("A101", "123 Nguyễn Trãi, Quận 5, TP.HCM", BigDecimal("2500000"),
        listOf("Wi-Fi", "Máy lạnh", "Chỗ để xe"), "Nguyễn Văn A", "0901234567")

    val contract = MyContract(1, "ACTIVE", "2026-06-01", "2027-05-31",
        room.rentPrice, BigDecimal("2500000"),
        "Tiền thuê thanh toán theo tháng. Người thuê thông báo trước khi kết thúc hợp đồng.")

    private val currentInvoice = TenantInvoice(
        101, "HD-2026-11-A101", "2026-11", TenantInvoiceStatus.UNPAID, "2026-12-05",
        BigDecimal("2500000"), BigDecimal("180000"), BigDecimal("70000"), BigDecimal("50000"),
        BigDecimal("50000"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
        BigDecimal("2850000"), BigDecimal.ZERO, BigDecimal("2850000")
    )
    private val invoices = listOf(currentInvoice, currentInvoice.copy(
        id = 100, invoiceCode = "HD-2026-10-A101", billingMonth = "2026-10", dueDate = "2026-11-05",
        status = TenantInvoiceStatus.PAID, paidAmount = currentInvoice.totalAmount, remainingAmount = BigDecimal.ZERO
    ))

    fun invoice(id: Long) = invoices.find { it.id == id }

    fun invoicePage(month: String?, status: TenantInvoiceStatus?, page: PageResponse<TenantInvoice>): PageResponse<TenantInvoice> {
        val matching = invoices.filter { (month == null || it.billingMonth == month) && (status == null || it.status == status) }
        val offset = (page.page.toLong() * page.size).coerceAtMost(matching.size.toLong()).toInt()
        return PageResponse(matching.drop(offset).take(page.size), page.page, page.size, matching.size.toLong(),
            (matching.size + page.size - 1) / page.size)
    }

    fun fillDashboard(dashboard: TenantDashboard) = dashboard.copy(
        roomCode = dashboard.roomCode ?: room.roomCode,
        amountDue = dashboard.amountDue ?: currentInvoice.remainingAmount,
        dueDate = dashboard.dueDate ?: currentInvoice.dueDate
    )
}
