package com.example.rental_property_management_app

import com.example.rental_property_management_app.utils.ValidationUtils
import org.junit.Assert.*
import org.junit.Test

class EmailPhoneOtpValidationUnitTest {

    @Test
    fun isValidGmail_acceptsGmailOnlyAndRejectsOtherDomains() {
        assertTrue(ValidationUtils.isValidGmail("nguyenvana@gmail.com"))
        assertTrue(ValidationUtils.isValidGmail("  User.Name+01@GMAIL.COM  "))
        assertFalse(ValidationUtils.isValidGmail(""))
        assertFalse(ValidationUtils.isValidGmail("user@yahoo.com"))
        assertFalse(ValidationUtils.isValidGmail("user@outlook.com"))
        assertFalse(ValidationUtils.isValidGmail("invalid-email"))
        assertNull(ValidationUtils.getGmailError("valid.user@gmail.com"))
        assertNotNull(ValidationUtils.getGmailError("valid.user@yahoo.com"))
    }

    @Test
    fun normalizeVietnamPhoneToE164_handlesLocalIntlAndSeparators() {
        assertEquals("+84987654321", ValidationUtils.normalizeVietnamPhoneToE164("0987654321"))
        assertEquals("+84912345678", ValidationUtils.normalizeVietnamPhoneToE164("+84 912-345.678"))
        assertEquals("+84351234567", ValidationUtils.normalizeVietnamPhoneToE164("84351234567"))
        assertEquals("+84701234567", ValidationUtils.normalizeVietnamPhoneToE164("0701234567"))
        assertEquals("+84561234567", ValidationUtils.normalizeVietnamPhoneToE164("0561234567"))
        assertEquals("+84891234567", ValidationUtils.normalizeVietnamPhoneToE164("0891234567"))

        assertNull(ValidationUtils.normalizeVietnamPhoneToE164(""))
        assertNull(ValidationUtils.normalizeVietnamPhoneToE164("0123456789"))
        assertNull(ValidationUtils.normalizeVietnamPhoneToE164("09876543"))
        assertNull(ValidationUtils.normalizeVietnamPhoneToE164("+14155552671"))
    }

    @Test
    fun isValidVietnamPhone_andErrorMessages_workCorrectly() {
        assertTrue(ValidationUtils.isValidVietnamPhone("0987654321"))
        assertTrue(ValidationUtils.isValidVietnamPhone("+84987654321"))
        assertFalse(ValidationUtils.isValidVietnamPhone("02838221234"))
        assertNull(ValidationUtils.getVietnamPhoneError("0987654321"))
        assertNotNull(ValidationUtils.getVietnamPhoneError("12345"))
    }

    @Test
    fun isValidOtp_requiresExactlySixDigits() {
        assertTrue(ValidationUtils.isValidOtp("012345"))
        assertTrue(ValidationUtils.isValidOtp(" 654321 "))
        assertFalse(ValidationUtils.isValidOtp(""))
        assertFalse(ValidationUtils.isValidOtp("12345"))
        assertFalse(ValidationUtils.isValidOtp("1234567"))
        assertFalse(ValidationUtils.isValidOtp("12a456"))
        assertNull(ValidationUtils.getOtpError("123456"))
        assertNotNull(ValidationUtils.getOtpError("1234"))
    }

    @Test
    fun isValidEmailOrPhone_acceptsVerifiedEmailOrVietnamPhoneLogin() {
        assertTrue(ValidationUtils.isValidEmailOrPhone("landlord1@example.com"))
        assertTrue(ValidationUtils.isValidEmailOrPhone("tenant@gmail.com"))
        assertTrue(ValidationUtils.isValidEmailOrPhone("0901234567"))
        assertTrue(ValidationUtils.isValidEmailOrPhone("+84987654321"))
        assertFalse(ValidationUtils.isValidEmailOrPhone(""))
        assertFalse(ValidationUtils.isValidEmailOrPhone("abc"))
        assertNull(ValidationUtils.getEmailOrPhoneError("+84987654321"))
        assertNotNull(ValidationUtils.getEmailOrPhoneError("invalid!"))
    }
}
