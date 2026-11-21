package com.example.rental_property_management_app

import com.example.rental_property_management_app.models.ContractStatus
import com.example.rental_property_management_app.models.InvoiceStatus
import com.example.rental_property_management_app.models.WaterBillingMode
import com.example.rental_property_management_app.utils.CccdQrParser
import org.junit.Assert.*
import org.junit.Test

class ContractInvoiceCccdUnitTest {

    @Test
    fun cccdQrParser_parsesStandard7FieldPayload() {
        val raw = "079200001234|025123456|Nguyễn Văn An|15081998|Nam|123 Nguyễn Trãi, Q5, TP.HCM|01012022"
        val parsed = CccdQrParser.parse(raw)
        assertNotNull(parsed)
        assertEquals("079200001234", parsed!!.identityNumber)
        assertEquals("Nguyễn Văn An", parsed.fullName)
        assertEquals("1998-08-15", parsed.dateOfBirthIso)
        assertEquals("Nam", parsed.gender)
        assertEquals("123 Nguyễn Trãi, Q5, TP.HCM", parsed.address)
    }

    @Test
    fun cccdQrParser_parses6FieldAndRejectsMalformedSafely() {
        val raw6 = "079099004321|Trần Thị Bình|05022001|Nữ|Quận 1, TP.HCM|10102023"
        val parsed6 = CccdQrParser.parse(raw6)
        assertNotNull(parsed6)
        assertEquals("079099004321", parsed6!!.identityNumber)
        assertEquals("Trần Thị Bình", parsed6.fullName)
        assertEquals("2001-02-05", parsed6.dateOfBirthIso)

        assertNull(CccdQrParser.parse(null))
        assertNull(CccdQrParser.parse(""))
        assertNull(CccdQrParser.parse("invalid-without-pipes"))
        assertNull(CccdQrParser.parse("12345|ShortId|Test|01012000|Nam|HCM|01012020"))
    }

    @Test
    fun invoiceAndContractEnums_exposeCompleteVietnameseLabelsAndColors() {
        assertEquals("Nháp", InvoiceStatus.DRAFT.getDisplayName())
        assertEquals("Đã hủy", InvoiceStatus.CANCELLED.getDisplayName())
        assertTrue(InvoiceStatus.DRAFT.getTextColorRes() != InvoiceStatus.PAID.getTextColorRes())
        assertTrue(InvoiceStatus.CANCELLED.getBgColorRes() != InvoiceStatus.UNPAID.getBgColorRes())

        assertEquals("Nháp", ContractStatus.DRAFT.getDisplayName())
        assertEquals("Đã ký (Chờ hiệu lực)", ContractStatus.SIGNED.getDisplayName())
        assertEquals("Đang hiệu lực", ContractStatus.ACTIVE.getDisplayName())
        assertEquals("Hết hạn", ContractStatus.EXPIRED.getDisplayName())
        assertEquals("Đã thanh lý", ContractStatus.TERMINATED.getDisplayName())

        assertEquals("Theo đồng hồ (m³)", WaterBillingMode.METERED.getDisplayName())
        assertEquals("Khoán theo phòng", WaterBillingMode.FIXED.getDisplayName())
        assertEquals("Theo đầu người", WaterBillingMode.PER_PERSON.getDisplayName())
    }
}
