package com.example.rental_property_management_app.repositories

import com.example.rental_property_management_app.models.TenantDashboard
import java.math.BigDecimal

internal object MockData {
    fun fillDashboard(dashboard: TenantDashboard) = dashboard.copy(
        roomCode = dashboard.roomCode ?: "A101",
        amountDue = dashboard.amountDue ?: BigDecimal("2850000"),
        dueDate = dashboard.dueDate ?: "2026-12-05"
    )
}
