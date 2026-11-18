package com.example.rental_property_management_app.models

import com.google.gson.annotations.SerializedName

/**
 * Tenant Response từ Backend
 */
data class TenantResponse(
    @SerializedName("id")
    val id: Long,
    
    @SerializedName("userId")
    val userId: Long?,
    
    @SerializedName("landlordId")
    val landlordId: Long,
    
    @SerializedName("propertyId")
    val propertyId: Long,
    
    @SerializedName("propertyName")
    val propertyName: String,
    
    @SerializedName("fullName")
    val fullName: String,
    
    @SerializedName("phone")
    val phone: String,
    
    @SerializedName("email")
    val email: String?,
    
    @SerializedName("identityNumber")
    val identityNumber: String,      // Masked trong list, full trong detail
    
    @SerializedName("dateOfBirth")
    val dateOfBirth: String?,
    
    @SerializedName("address")
    val address: String?,
    
    @SerializedName("emergencyContact")
    val emergencyContact: String?,
    
    @SerializedName("currentRoom")
    val currentRoom: String?,        // TODO: Phase 3
    
    @SerializedName("currentContract")
    val currentContract: String?,    // TODO: Phase 3
    
    @SerializedName("createdAt")
    val createdAt: String,
    
    @SerializedName("updatedAt")
    val updatedAt: String
)

/**
 * Create Tenant Request
 */
data class CreateTenantRequest(
    @SerializedName("propertyId")
    val propertyId: Long,
    
    @SerializedName("fullName")
    val fullName: String,
    
    @SerializedName("phone")
    val phone: String,
    
    @SerializedName("email")
    val email: String?,
    
    @SerializedName("identityNumber")
    val identityNumber: String,
    
    @SerializedName("dateOfBirth")
    val dateOfBirth: String?,
    
    @SerializedName("address")
    val address: String?,
    
    @SerializedName("emergencyContact")
    val emergencyContact: String?
)

/**
 * Update Tenant Request
 */
data class UpdateTenantRequest(
    @SerializedName("fullName")
    val fullName: String,
    
    @SerializedName("phone")
    val phone: String,
    
    @SerializedName("email")
    val email: String?,
    
    @SerializedName("identityNumber")
    val identityNumber: String,
    
    @SerializedName("dateOfBirth")
    val dateOfBirth: String?,
    
    @SerializedName("address")
    val address: String?,
    
    @SerializedName("emergencyContact")
    val emergencyContact: String?
)
