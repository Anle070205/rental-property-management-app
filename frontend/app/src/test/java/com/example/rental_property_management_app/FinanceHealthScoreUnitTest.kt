package com.example.rental_property_management_app

import com.example.rental_property_management_app.models.InvoiceStatus
import com.example.rental_property_management_app.models.PaymentMethod
import com.example.rental_property_management_app.utils.CurrencyUtils
import org.junit.Assert.*
import org.junit.Test
import java.math.BigDecimal

class FinanceHealthScoreUnitTest {

    @Test
    fun formatVND_bigDecimal_formatsAccurately() {
        assertEquals("3.650.000 ₫", CurrencyUtils.formatVND(BigDecimal("3650000.00")))
        assertEquals("0 ₫", CurrencyUtils.formatVND(null as BigDecimal?))
    }

    @Test
    fun invoiceStatus_displaysCorrectVietnameseLabels() {
        assertEquals("Chưa thanh toán", InvoiceStatus.UNPAID.getDisplayName())
        assertEquals("Thanh toán 1 phần", InvoiceStatus.PARTIALLY_PAID.getDisplayName())
        assertEquals("Đã thanh toán", InvoiceStatus.PAID.getDisplayName())
        assertEquals("Quá hạn", InvoiceStatus.OVERDUE.getDisplayName())
    }

    @Test
    fun paymentMethod_displaysCorrectLabels() {
        assertEquals("Tiền mặt", PaymentMethod.CASH.getDisplayName())
        assertEquals("Chuyển khoản", PaymentMethod.BANK_TRANSFER.getDisplayName())
    }

    @Test
    fun utilityCalculation_usesExactBigDecimalArithmetic() {
        val elecUsage = BigDecimal("1345").subtract(BigDecimal("1225"))
        val waterUsage = BigDecimal("88").subtract(BigDecimal("80"))
        val elecCost = elecUsage.multiply(BigDecimal("3500"))
        val waterCost = waterUsage.multiply(BigDecimal("15000"))
        val total = elecCost.add(waterCost).add(BigDecimal("100000")).add(BigDecimal("50000"))

        assertEquals(BigDecimal("120"), elecUsage)
        assertEquals(BigDecimal("8"), waterUsage)
        assertEquals(BigDecimal("690000"), total)
    }
}
