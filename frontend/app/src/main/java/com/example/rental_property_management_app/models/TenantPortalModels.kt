package com.example.rental_property_management_app.models

import java.math.BigDecimal

data class TenantDashboard(
    val fullName: String,
    val roomCode: String?,
    val amountDue: BigDecimal?,
    val dueDate: String?,
    val unreadCount: Long
)
