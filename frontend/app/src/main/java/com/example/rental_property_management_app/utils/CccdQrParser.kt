package com.example.rental_property_management_app.utils

/**
 * Tiện ích giải mã chuỗi QR trên thẻ CCCD gắn chip Việt Nam.
 * Định dạng chuẩn: SoCCCD|SoCMNDCu|HoTen|ddMMyyyy|GioiTinh|DiaChiThuongTru|NgayCap
 * (hoặc 6 trường nếu không có số CMND cũ).
 */
data class ParsedCccdInfo(
    val identityNumber: String,
    val fullName: String,
    val dateOfBirthIso: String?,
    val gender: String?,
    val address: String?
)

object CccdQrParser {

    fun parse(rawPayload: String?): ParsedCccdInfo? {
        val trimmed = rawPayload?.trim().orEmpty()
        if (trimmed.isEmpty() || !trimmed.contains('|')) {
            return null
        }

        val parts = trimmed.split('|').map { it.trim() }
        if (parts.size < 6) {
            return null
        }

        val cccd = parts[0].filter { it.isDigit() }
        if (cccd.length != 9 && cccd.length != 12) {
            return null
        }

        // Hỗ trợ cả định dạng 7 phần (có CMND cũ ở index 1) và 6 phần (Họ tên ở index 1)
        val hasOldIdSlot = parts.size >= 7
        val fullName = if (hasOldIdSlot) parts[2] else parts[1]
        if (fullName.isBlank()) {
            return null
        }

        val rawDob = if (hasOldIdSlot) parts[3] else parts[2]
        val gender = (if (hasOldIdSlot) parts[4] else parts[3]).takeIf { it.isNotBlank() }
        val address = (if (hasOldIdSlot) parts[5] else parts[4]).takeIf { it.isNotBlank() }

        return ParsedCccdInfo(
            identityNumber = cccd,
            fullName = fullName,
            dateOfBirthIso = normalizeDdMmYyyyToIso(rawDob),
            gender = gender,
            address = address
        )
    }

    private fun normalizeDdMmYyyyToIso(raw: String): String? {
        val digits = raw.filter { it.isDigit() }
        if (digits.length != 8) {
            return null
        }
        val day = digits.substring(0, 2).toIntOrNull() ?: return null
        val month = digits.substring(2, 4).toIntOrNull() ?: return null
        val year = digits.substring(4, 8).toIntOrNull() ?: return null
        if (year < 1900 || month !in 1..12 || day !in 1..31) {
            return null
        }
        return "%04d-%02d-%02d".format(year, month, day)
    }
}
