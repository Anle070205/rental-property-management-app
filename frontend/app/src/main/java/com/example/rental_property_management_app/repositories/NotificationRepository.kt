package com.example.rental_property_management_app.repositories

import com.example.rental_property_management_app.models.NotificationType
import com.example.rental_property_management_app.network.RetrofitClient

class NotificationRepository {
    private val api = RetrofitClient.apiService

    suspend fun list(type: NotificationType?, unreadOnly: Boolean, page: Int) =
        apiResult({ api.notifications(type, unreadOnly, page) }) { it.data }

    suspend fun markRead(id: Long) = apiResult({ api.markNotificationRead(id) }) { Unit }

    suspend fun markAllRead() = apiResult({ api.markAllNotificationsRead() }) { Unit }
}
