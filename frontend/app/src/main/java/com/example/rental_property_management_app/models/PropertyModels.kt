package com.example.rental_property_management_app.models

import com.google.gson.annotations.SerializedName

/**
 * Property Response từ Backend
 */
data class PropertyResponse(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("landlordId")
    val landlordId: Long,
    
    @SerializedName("landlordName")
    val landlordName: String,
    
    @SerializedName("name")
    val name: String,
    
    @SerializedName("address")
    val address: String,
    
    @SerializedName("description")
    val description: String?,
    
    @SerializedName("totalRooms")
    val totalRooms: Int,
    
    @SerializedName("vacantRooms")
    val vacantRooms: Int,
    
    @SerializedName("occupiedRooms")
    val occupiedRooms: Int,
    
    @SerializedName("createdAt")
    val createdAt: String,
    
    @SerializedName("updatedAt")
    val updatedAt: String
)

/**
 * Create Property Request
 */
data class CreatePropertyRequest(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("address")
    val address: String,
    
    @SerializedName("description")
    val description: String?
)

/**
 * Update Property Request
 */
data class UpdatePropertyRequest(
    @SerializedName("name")
    val name: String,
    
    @SerializedName("address")
    val address: String,
    
    @SerializedName("description")
    val description: String?
)
