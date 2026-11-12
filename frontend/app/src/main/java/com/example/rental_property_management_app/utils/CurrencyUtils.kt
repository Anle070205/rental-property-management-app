package com.example.rental_property_management_app.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.*

/**
 * Utility class để format tiền tệ VND
 */
object CurrencyUtils {
    
    private val vndFormat: DecimalFormat by lazy {
        val symbols = DecimalFormatSymbols(Locale("vi", "VN")).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }
        DecimalFormat("#,###", symbols)
    }
    
    /**
     * Format số thành chuỗi tiền VND với ký hiệu ₫
     * Ví dụ: 1000000 -> "1.000.000 ₫"
     */
    fun formatVND(amount: Long): String {
        return "${vndFormat.format(amount)} ₫"
    }
    
    /**
     * Format số thành chuỗi tiền VND với ký hiệu ₫
     * Ví dụ: 1000000.0 -> "1.000.000 ₫"
     */
    fun formatVND(amount: Double): String {
        return "${vndFormat.format(amount.toLong())} ₫"
    }
    
    /**
     * Format số thành chuỗi tiền VND không có ký hiệu
     * Ví dụ: 1000000 -> "1.000.000"
     */
    fun formatVNDWithoutSymbol(amount: Long): String {
        return vndFormat.format(amount)
    }
    
    /**
     * Parse chuỗi tiền VND thành số
     * Ví dụ: "1.000.000 ₫" -> 1000000
     */
    fun parseVND(amountString: String): Long? {
        return try {
            val cleanString = amountString
                .replace("₫", "")
                .replace(".", "")
                .replace(",", "")
                .trim()
            cleanString.toLongOrNull()
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Format số thành chuỗi rút gọn
     * Ví dụ: 1500000 -> "1,5 triệu"
     */
    fun formatVNDShort(amount: Long): String {
        return when {
            amount >= 1_000_000_000 -> {
                val billions = amount / 1_000_000_000.0
                String.format(Locale("vi", "VN"), "%.1f tỷ", billions)
            }
            amount >= 1_000_000 -> {
                val millions = amount / 1_000_000.0
                String.format(Locale("vi", "VN"), "%.1f triệu", millions)
            }
            amount >= 1_000 -> {
                val thousands = amount / 1_000.0
                String.format(Locale("vi", "VN"), "%.1f nghìn", thousands)
            }
            else -> formatVND(amount)
        }
    }
    
    /**
     * Validate if string is valid VND amount
     */
    fun isValidAmount(amountString: String): Boolean {
        return parseVND(amountString) != null
    }
    
    /**
     * Format with prefix (for display in bills, invoices)
     * Ví dụ: 1000000 -> "Tổng cộng: 1.000.000 ₫"
     */
    fun formatWithPrefix(amount: Long, prefix: String = "Tổng cộng"): String {
        return "$prefix: ${formatVND(amount)}"
    }
}
