package com.example.rental_property_management_app.repositories

import com.example.rental_property_management_app.models.MyRoom
import com.example.rental_property_management_app.models.TenantDashboard
import java.math.BigDecimal

internal object MockData {
    val room = MyRoom("A101", "123 Nguyễn Trãi, Quận 5, TP.HCM", BigDecimal("2500000"),
        listOf("Wi-Fi", "Máy lạnh", "Chỗ để xe"), "Nguyễn Văn A", "0901234567")

    fun fillDashboard(dashboard: TenantDashboard) = dashboard.copy(
        roomCode = dashboard.roomCode ?: room.roomCode,
        amountDue = dashboard.amountDue ?: BigDecimal("2850000"),
        dueDate = dashboard.dueDate ?: "2026-12-05"
    )
}
