package com.example.rental_property_management_app

import com.example.rental_property_management_app.utils.CccdQrParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class CccdQrParserUnitTest {

    @Test
    fun parseSevenFieldCccdQr_extractsVerifiedFields() {
        val raw = "079200001234|025123456|Lê Minh Khách|10051999|Nam|123 Lê Lợi, Q1, TP.HCM|01012022"
        val parsed = CccdQrParser.parse(raw)
        assertNotNull(parsed)
        assertEquals("079200001234", parsed!!.identityNumber)
        assertEquals("Lê Minh Khách", parsed.fullName)
        assertEquals("1999-05-10", parsed.dateOfBirthIso)
        assertEquals("Nam", parsed.gender)
    }

    @Test
    fun parseSixFieldCccdQr_extractsVerifiedFields() {
        val raw = "079200005678|Nguyễn Thị Thân|01011975|Nữ|Hà Nội|15062021"
        val parsed = CccdQrParser.parse(raw)
        assertNotNull(parsed)
        assertEquals("079200005678", parsed!!.identityNumber)
        assertEquals("Nguyễn Thị Thân", parsed.fullName)
        assertEquals("1975-01-01", parsed.dateOfBirthIso)
        assertEquals("Nữ", parsed.gender)
    }

    @Test
    fun parseInvalidOrIncompleteQr_returnsNullWithoutFabricating() {
        assertNull(CccdQrParser.parse(null))
        assertNull(CccdQrParser.parse("invalid-qr-payload"))
        assertNull(CccdQrParser.parse("12345||Short ID|10051999|Nam|HCM|01012022"))
        assertNull(CccdQrParser.parse("079200001234|||10051999|Nam|HCM|01012022"))
    }
}
