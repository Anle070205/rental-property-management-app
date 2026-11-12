package com.example.rental_property_management_app.utils

import java.text.SimpleDateFormat
import java.util.*

/**
 * Utility class để format date/time theo định dạng Việt Nam
 */
object DateUtils {
    
    /**
     * Format Date to Vietnamese date string (dd/MM/yyyy)
     */
    fun formatDate(date: Date): String {
        val formatter = SimpleDateFormat(Constants.DATE_FORMAT, Locale("vi", "VN"))
        return formatter.format(date)
    }
    
    /**
     * Format Date to Vietnamese datetime string (dd/MM/yyyy HH:mm)
     */
    fun formatDateTime(date: Date): String {
        val formatter = SimpleDateFormat(Constants.DATETIME_FORMAT, Locale("vi", "VN"))
        return formatter.format(date)
    }
    
    /**
     * Format Date to time string (HH:mm)
     */
    fun formatTime(date: Date): String {
        val formatter = SimpleDateFormat(Constants.TIME_FORMAT, Locale("vi", "VN"))
        return formatter.format(date)
    }
    
    /**
     * Parse date string from API format (yyyy-MM-dd) to Date
     */
    fun parseApiDate(dateString: String): Date? {
        return try {
            val formatter = SimpleDateFormat(Constants.API_DATE_FORMAT, Locale.US)
            formatter.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Parse datetime string from API format (yyyy-MM-dd'T'HH:mm:ss) to Date
     */
    fun parseApiDateTime(dateTimeString: String): Date? {
        return try {
            val formatter = SimpleDateFormat(Constants.API_DATETIME_FORMAT, Locale.US)
            formatter.parse(dateTimeString)
        } catch (e: Exception) {
            null
        }
    }
    
    /**
     * Format Date to API date format (yyyy-MM-dd)
     */
    fun formatToApiDate(date: Date): String {
        val formatter = SimpleDateFormat(Constants.API_DATE_FORMAT, Locale.US)
        return formatter.format(date)
    }
    
    /**
     * Format Date to API datetime format (yyyy-MM-dd'T'HH:mm:ss)
     */
    fun formatToApiDateTime(date: Date): String {
        val formatter = SimpleDateFormat(Constants.API_DATETIME_FORMAT, Locale.US)
        return formatter.format(date)
    }
    
    /**
     * Get current date as formatted string
     */
    fun getCurrentDate(): String {
        return formatDate(Date())
    }
    
    /**
     * Get current datetime as formatted string
     */
    fun getCurrentDateTime(): String {
        return formatDateTime(Date())
    }
    
    /**
     * Check if date is today
     */
    fun isToday(date: Date): Boolean {
        val today = Calendar.getInstance()
        val dateCalendar = Calendar.getInstance().apply { time = date }
        
        return today.get(Calendar.YEAR) == dateCalendar.get(Calendar.YEAR) &&
               today.get(Calendar.DAY_OF_YEAR) == dateCalendar.get(Calendar.DAY_OF_YEAR)
    }
    
    /**
     * Get relative time string (e.g., "2 giờ trước", "3 ngày trước")
     */
    fun getRelativeTimeString(date: Date): String {
        val now = Date()
        val diff = now.time - date.time
        
        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24
        
        return when {
            seconds < 60 -> "Vừa xong"
            minutes < 60 -> "$minutes phút trước"
            hours < 24 -> "$hours giờ trước"
            days < 30 -> "$days ngày trước"
            else -> formatDate(date)
        }
    }
}
