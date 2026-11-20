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
    val phone: String?,

    @SerializedName("email")
    val email: String?,

    @SerializedName("identityNumber")
    val identityNumber: String?,

    @SerializedName("dateOfBirth")
    val dateOfBirth: String?,

    @SerializedName("address")
    val address: String?,

    @SerializedName("emergencyContact")
    val emergencyContact: String?,

    @SerializedName("currentRoom")
    val currentRoom: String?,

    @SerializedName("currentContract")
    val currentContract: String?,

    @SerializedName("currentRoomId")
    val currentRoomId: Long? = null,

    @SerializedName("currentContractId")
    val currentContractId: Long? = null,

    @SerializedName("accountStatus")
    val accountStatus: String? = null,

    @SerializedName("roommateCount")
    val roommateCount: Long = 0,

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
    val phone: String? = null,

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("identityNumber")
    val identityNumber: String? = null,

    @SerializedName("consentConfirmed")
    val consentConfirmed: Boolean = true,

    @SerializedName("dateOfBirth")
    val dateOfBirth: String? = null,

    @SerializedName("address")
    val address: String? = null,

    @SerializedName("emergencyContact")
    val emergencyContact: String? = null
)

/**
 * Update Tenant Request
 */
data class UpdateTenantRequest(
    @SerializedName("fullName")
    val fullName: String,

    @SerializedName("phone")
    val phone: String? = null,

    @SerializedName("email")
    val email: String? = null,

    @SerializedName("identityNumber")
    val identityNumber: String? = null,

    @SerializedName("consentConfirmed")
    val consentConfirmed: Boolean = true,

    @SerializedName("dateOfBirth")
    val dateOfBirth: String? = null,

    @SerializedName("address")
    val address: String? = null,

    @SerializedName("emergencyContact")
    val emergencyContact: String? = null
)

data class ProvisionTenantAccountRequest(
    @SerializedName("channel")
    val channel: String,

    @SerializedName("target")
    val target: String,

    @SerializedName("confirmLinkExisting")
    val confirmLinkExisting: Boolean = false
)

data class ProvisionTenantAccountResponse(
    @SerializedName("tenantId")
    val tenantId: Long,

    @SerializedName("userId")
    val userId: Long?,

    @SerializedName("accountStatus")
    val accountStatus: String,

    @SerializedName("action")
    val action: String,

    @SerializedName("channel")
    val channel: String,

    @SerializedName("target")
    val target: String,

    @SerializedName("resendCooldownSeconds")
    val resendCooldownSeconds: Int = 60,

    @SerializedName("message")
    val message: String
)
