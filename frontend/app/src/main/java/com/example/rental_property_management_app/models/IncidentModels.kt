package com.example.rental_property_management_app.models

enum class IncidentType { ELECTRIC, WATER, EQUIPMENT, SECURITY, OTHER }
enum class IncidentPriority { LOW, MEDIUM, HIGH, URGENT }
enum class IncidentStatus { RECEIVED, IN_PROGRESS, DONE }

data class Incident(
    val id: Long,
    val type: IncidentType,
    val title: String,
    val description: String,
    val priority: IncidentPriority,
    val status: IncidentStatus,
    val imageUrl: String?,
    val landlordResponse: String?,
    val createdAt: String,
    val updatedAt: String
)

data class CreateIncidentRequest(
    val type: IncidentType, val title: String, val description: String,
    val priority: IncidentPriority, val imageUrl: String?
)

data class UpdateIncidentRequest(val description: String?, val imageUrl: String?)
