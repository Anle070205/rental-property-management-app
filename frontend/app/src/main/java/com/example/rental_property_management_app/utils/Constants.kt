package com.example.rental_property_management_app.utils

object Constants {
    
    // API Configuration
    const val BASE_URL = "http://10.0.2.2:8081/" // Android emulator host loopback
    const val API_PREFIX = "api/"
    const val USE_MOCK_TENANT_DATA = true
    
    // API Endpoints
    object Endpoints {
        // Authentication
        const val REGISTER = "auth/register"
        const val LOGIN = "auth/login"
        const val LOGOUT = "auth/logout"
        const val FORGOT_PASSWORD = "auth/forgot-password"
        const val REFRESH_TOKEN = "auth/refresh-token"
        
        // Test
        const val TEST = "test"
    }
    
    // SharedPreferences Keys
    object PrefsKeys {
        const val PREFS_NAME = "RoomCarePrefs"
        const val KEY_ACCESS_TOKEN = "access_token"
        const val KEY_REFRESH_TOKEN = "refresh_token"
        const val KEY_USER_ID = "user_id"
        const val KEY_USER_ROLE = "user_role"
        const val KEY_USER_NAME = "user_name"
        const val KEY_USER_EMAIL = "user_email"
        const val KEY_IS_LOGGED_IN = "is_logged_in"
    }
    
    // User Roles
    object Roles {
        const val LANDLORD = "LANDLORD"
        const val TENANT = "TENANT"

        const val ADMIN = "ADMIN"

    }
    
    // Network
    const val CONNECT_TIMEOUT = 30L // seconds
    const val READ_TIMEOUT = 30L // seconds
    const val WRITE_TIMEOUT = 30L // seconds
    
    // Validation
    const val MIN_PASSWORD_LENGTH = 6
    const val MAX_PASSWORD_LENGTH = 50
    const val PHONE_LENGTH = 10
    
    // Date & Time Format
    const val DATE_FORMAT = "dd/MM/yyyy"
    const val TIME_FORMAT = "HH:mm"
    const val DATETIME_FORMAT = "dd/MM/yyyy HH:mm"
    const val API_DATE_FORMAT = "yyyy-MM-dd"
    const val API_DATETIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss"
    
    // Request Codes
    const val REQUEST_CODE_PERMISSION = 1001
    
    // Error Messages
    object ErrorMessages {
        const val NETWORK_ERROR = "Lỗi kết nối mạng"
        const val SERVER_ERROR = "Lỗi máy chủ"
        const val UNKNOWN_ERROR = "Lỗi không xác định"
        const val UNAUTHORIZED = "Phiên đăng nhập hết hạn"
        const val VALIDATION_ERROR = "Dữ liệu không hợp lệ"
    }
}
