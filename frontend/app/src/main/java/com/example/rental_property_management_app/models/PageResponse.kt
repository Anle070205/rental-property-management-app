package com.example.rental_property_management_app.models

import com.google.gson.annotations.SerializedName

/**
 * Generic Pagination Response wrapper
 * Khớp với PageResponse<T> từ Backend
 */
data class PageResponse<T>(
    @SerializedName("content")
    val content: List<T>,
    
    @SerializedName("page")
    val page: Int,
    
    @SerializedName("size")
    val size: Int,
    
    @SerializedName("totalElements")
    val totalElements: Long,
    
    @SerializedName("totalPages")
    val totalPages: Int
)
