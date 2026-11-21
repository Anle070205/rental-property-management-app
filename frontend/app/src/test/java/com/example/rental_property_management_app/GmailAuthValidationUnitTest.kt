package com.example.rental_property_management_app

import com.example.rental_property_management_app.utils.ValidationUtils
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class GmailAuthValidationUnitTest {

    @Test
    fun isValidGmail_acceptsRealGmailAddressesAndRejectsOthers() {
        assertTrue(ValidationUtils.isValidGmail("sinhvien.k24@gmail.com"))
        assertTrue(ValidationUtils.isValidGmail("  ChuNha.RoomCare+01@GMAIL.COM "))

        assertFalse(ValidationUtils.isValidGmail(""))
        assertFalse(ValidationUtils.isValidGmail("not-an-email"))
        assertFalse(ValidationUtils.isValidGmail("user@yahoo.com"))
        assertFalse(ValidationUtils.isValidGmail("user@gmail.com.vn"))
    }

    @Test
    fun getGmailError_returnsVietnameseMessages() {
        assertNull(ValidationUtils.getGmailError("roomcare.user@gmail.com"))
        assertEquals("Địa chỉ Gmail không được để trống", ValidationUtils.getGmailError("   "))
        assertEquals("Địa chỉ email không hợp lệ", ValidationUtils.getGmailError("roomcare.user"))
        assertEquals(
            "Vui lòng nhập địa chỉ Gmail (@gmail.com) đang hoạt động",
            ValidationUtils.getGmailError("roomcare.user@outlook.com")
        )
    }

    @Test
    fun isValidOtp_andGetOtpError_validateSixDigitsStrictly() {
        assertTrue(ValidationUtils.isValidOtp("012345"))
        assertTrue(ValidationUtils.isValidOtp(" 987654 "))
        assertNull(ValidationUtils.getOtpError("456789"))

        assertFalse(ValidationUtils.isValidOtp("12345"))
        assertFalse(ValidationUtils.isValidOtp("1234567"))
        assertFalse(ValidationUtils.isValidOtp("12a456"))
        assertNotNull(ValidationUtils.getOtpError(""))
        assertNotNull(ValidationUtils.getOtpError("1234"))
    }

    @Test
    fun getConfirmPasswordError_validatesMatchCorrectly() {
        assertNull(ValidationUtils.getConfirmPasswordError("secret123", "secret123"))
        assertEquals("Mật khẩu không khớp", ValidationUtils.getConfirmPasswordError("secret123", "secret124"))
        assertEquals("Xác nhận mật khẩu không được để trống", ValidationUtils.getConfirmPasswordError("secret123", ""))
    }
}
