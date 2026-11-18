package com.example.rental_property_management_app.models

enum class NotificationType { INVOICE, CONTRACT, PAYMENT, INCIDENT, MAINTENANCE, SYSTEM }

data class NotificationItem(
    val id: Long,
    val type: NotificationType,
    val title: String,
    val content: String,
    val refId: Long?,
    val isRead: Boolean,
    val createdAt: String
)
