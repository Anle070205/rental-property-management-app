package com.example.rental_property_management_app.repositories

import com.example.rental_property_management_app.models.*
import com.example.rental_property_management_app.network.RetrofitClient
import com.example.rental_property_management_app.utils.Resource

class AdminRepository {
    private val api = RetrofitClient.apiService

    suspend fun getDashboard(): Resource<AdminDashboardResponse> =
        ApiResultHandler.execute { api.getAdminDashboard() }

    suspend fun getAccounts(
        role: String? = null,
        isActive: Boolean? = null,
        keyword: String? = null,
        page: Int = 0,
        size: Int = 10
    ): Resource<AdminAccountPageResult> =
        ApiResultHandler.execute { api.getAdminAccounts(role, isActive, keyword, page, size) }

    suspend fun getAccountDetail(id: Long): Resource<AdminAccountItem> =
        ApiResultHandler.execute { api.getAdminAccountDetail(id) }

    suspend fun createAccount(req: AdminCreateAccountRequest): Resource<AdminAccountItem> =
        ApiResultHandler.execute { api.createAdminAccount(req) }

    suspend fun updateAccount(id: Long, req: AdminUpdateAccountRequest): Resource<AdminAccountItem> =
        ApiResultHandler.execute { api.updateAdminAccount(id, req) }

    suspend fun lockAccount(id: Long): Resource<AdminAccountItem> =
        ApiResultHandler.execute { api.lockAdminAccount(id) }

    suspend fun unlockAccount(id: Long): Resource<AdminAccountItem> =
        ApiResultHandler.execute { api.unlockAdminAccount(id) }

    suspend fun getLandlords(keyword: String? = null): Resource<AdminLandlordListResult> =
        ApiResultHandler.execute { api.getAdminLandlords(keyword) }

    suspend fun getLandlordDetail(id: Long): Resource<AdminLandlordItem> =
        ApiResultHandler.execute { api.getAdminLandlordDetail(id) }

    suspend fun getProperties(keyword: String? = null): Resource<AdminPropertyListResult> =
        ApiResultHandler.execute { api.getAdminProperties(keyword) }

    suspend fun getPropertyDetail(id: Long): Resource<AdminPropertyItem> =
        ApiResultHandler.execute { api.getAdminPropertyDetail(id) }

    suspend fun getReports(keyword: String? = null, status: String? = null): Resource<AdminReportListResult> =
        ApiResultHandler.execute { api.getAdminReports(keyword, status) }

    suspend fun getReportDetail(id: Long): Resource<AdminReportItem> =
        ApiResultHandler.execute { api.getAdminReportDetail(id) }

    suspend fun resolveReport(id: Long, req: AdminResolveReportRequest): Resource<AdminReportItem> =
        ApiResultHandler.execute { api.resolveAdminReport(id, req) }

    suspend fun getAudienceCounts(): Resource<AdminBroadcastAudienceCounts> =
        ApiResultHandler.execute { api.getAdminBroadcastAudienceCounts() }

    suspend fun getBroadcasts(keyword: String? = null): Resource<AdminBroadcastListResult> =
        ApiResultHandler.execute { api.getAdminBroadcasts(keyword) }

    suspend fun getBroadcastDetail(id: Long): Resource<AdminBroadcastItem> =
        ApiResultHandler.execute { api.getAdminBroadcastDetail(id) }

    suspend fun createBroadcast(req: AdminBroadcastRequest, sendNow: Boolean): Resource<AdminBroadcastItem> =
        ApiResultHandler.execute { api.createAdminBroadcast(req, sendNow) }

    suspend fun updateBroadcast(id: Long, req: AdminBroadcastRequest): Resource<AdminBroadcastItem> =
        ApiResultHandler.execute { api.updateAdminBroadcast(id, req) }

    suspend fun sendBroadcast(id: Long): Resource<AdminBroadcastItem> =
        ApiResultHandler.execute { api.sendAdminBroadcast(id) }

    suspend fun changeAdminPassword(req: ChangePasswordRequest): Resource<AdminChangePasswordResponse> =
        ApiResultHandler.execute { api.changeAdminPassword(req) }
}
