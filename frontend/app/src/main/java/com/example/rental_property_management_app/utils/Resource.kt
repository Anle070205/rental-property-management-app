package com.example.rental_property_management_app.utils

/**
 * Wrapper class cho các trạng thái của data
 * Success: Data load thành công
 * Error: Có lỗi xảy ra
 * Loading: Đang load data
 */
sealed class Resource<T>(
    val data: T? = null,
    val message: String? = null
) {
    class Success<T>(data: T) : Resource<T>(data)
    class Error<T>(message: String, data: T? = null) : Resource<T>(data, message)
    class Loading<T>(data: T? = null) : Resource<T>(data)
}
