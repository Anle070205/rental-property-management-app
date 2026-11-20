package com.example.rental_property_management_app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rental_property_management_app.models.*
import com.example.rental_property_management_app.repositories.AdminRepository
import com.example.rental_property_management_app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AdminViewModel : ViewModel() {
    private val repo = AdminRepository()

    private val _dashboard = MutableStateFlow<Resource<AdminDashboardResponse>?>(null)
    val dashboard: StateFlow<Resource<AdminDashboardResponse>?> = _dashboard

    private val _accounts = MutableStateFlow<Resource<AdminAccountPageResult>?>(null)
    val accounts: StateFlow<Resource<AdminAccountPageResult>?> = _accounts

    private val _accountDetail = MutableStateFlow<Resource<AdminAccountItem>?>(null)
    val accountDetail: StateFlow<Resource<AdminAccountItem>?> = _accountDetail

    private val _accountAction = MutableStateFlow<Resource<AdminAccountItem>?>(null)
    val accountAction: StateFlow<Resource<AdminAccountItem>?> = _accountAction

    private val _landlords = MutableStateFlow<Resource<AdminLandlordListResult>?>(null)
    val landlords: StateFlow<Resource<AdminLandlordListResult>?> = _landlords

    private val _landlordDetail = MutableStateFlow<Resource<AdminLandlordItem>?>(null)
    val landlordDetail: StateFlow<Resource<AdminLandlordItem>?> = _landlordDetail

    private val _properties = MutableStateFlow<Resource<AdminPropertyListResult>?>(null)
    val properties: StateFlow<Resource<AdminPropertyListResult>?> = _properties

    private val _propertyDetail = MutableStateFlow<Resource<AdminPropertyItem>?>(null)
    val propertyDetail: StateFlow<Resource<AdminPropertyItem>?> = _propertyDetail

    private val _reports = MutableStateFlow<Resource<AdminReportListResult>?>(null)
    val reports: StateFlow<Resource<AdminReportListResult>?> = _reports

    private val _reportDetail = MutableStateFlow<Resource<AdminReportItem>?>(null)
    val reportDetail: StateFlow<Resource<AdminReportItem>?> = _reportDetail

    private val _broadcasts = MutableStateFlow<Resource<AdminBroadcastListResult>?>(null)
    val broadcasts: StateFlow<Resource<AdminBroadcastListResult>?> = _broadcasts

    private val _broadcastDetail = MutableStateFlow<Resource<AdminBroadcastItem>?>(null)
    val broadcastDetail: StateFlow<Resource<AdminBroadcastItem>?> = _broadcastDetail

    private val _audienceCounts = MutableStateFlow<Resource<AdminBroadcastAudienceCounts>?>(null)
    val audienceCounts: StateFlow<Resource<AdminBroadcastAudienceCounts>?> = _audienceCounts

    private val _passwordChange = MutableStateFlow<Resource<AdminChangePasswordResponse>?>(null)
    val passwordChange: StateFlow<Resource<AdminChangePasswordResponse>?> = _passwordChange

    fun loadDashboard() = viewModelScope.launch {
        _dashboard.value = Resource.Loading()
        _dashboard.value = repo.getDashboard()
    }

    fun loadAccounts(role: String? = null, isActive: Boolean? = null, keyword: String? = null, page: Int = 0) =
        viewModelScope.launch {
            _accounts.value = Resource.Loading()
            _accounts.value = repo.getAccounts(role, isActive, keyword, page, 10)
        }

    fun loadAccountDetail(id: Long) = viewModelScope.launch {
        _accountDetail.value = Resource.Loading()
        _accountDetail.value = repo.getAccountDetail(id)
    }

    fun createAccount(req: AdminCreateAccountRequest) = viewModelScope.launch {
        _accountAction.value = Resource.Loading()
        _accountAction.value = repo.createAccount(req)
    }

    fun updateAccount(id: Long, req: AdminUpdateAccountRequest) = viewModelScope.launch {
        _accountAction.value = Resource.Loading()
        _accountAction.value = repo.updateAccount(id, req)
    }

    fun lockAccount(id: Long) = viewModelScope.launch {
        _accountAction.value = Resource.Loading()
        val res = repo.lockAccount(id)
        _accountAction.value = res
        if (res is Resource.Success) _accountDetail.value = res
    }

    fun unlockAccount(id: Long) = viewModelScope.launch {
        _accountAction.value = Resource.Loading()
        val res = repo.unlockAccount(id)
        _accountAction.value = res
        if (res is Resource.Success) _accountDetail.value = res
    }

    fun resetAccountAction() {
        _accountAction.value = null
    }

    fun loadLandlords(keyword: String? = null) = viewModelScope.launch {
        _landlords.value = Resource.Loading()
        _landlords.value = repo.getLandlords(keyword)
    }

    fun loadLandlordDetail(id: Long) = viewModelScope.launch {
        _landlordDetail.value = Resource.Loading()
        _landlordDetail.value = repo.getLandlordDetail(id)
    }

    fun loadProperties(keyword: String? = null) = viewModelScope.launch {
        _properties.value = Resource.Loading()
        _properties.value = repo.getProperties(keyword)
    }

    fun loadPropertyDetail(id: Long) = viewModelScope.launch {
        _propertyDetail.value = Resource.Loading()
        _propertyDetail.value = repo.getPropertyDetail(id)
    }

    fun loadReports(keyword: String? = null, status: String? = null) = viewModelScope.launch {
        _reports.value = Resource.Loading()
        _reports.value = repo.getReports(keyword, status)
    }

    fun loadReportDetail(id: Long) = viewModelScope.launch {
        _reportDetail.value = Resource.Loading()
        _reportDetail.value = repo.getReportDetail(id)
    }

    fun resolveReport(id: Long, responseNote: String, status: String) = viewModelScope.launch {
        _reportDetail.value = Resource.Loading()
        _reportDetail.value = repo.resolveReport(id, AdminResolveReportRequest(responseNote, status))
    }

    fun loadBroadcasts(keyword: String? = null) = viewModelScope.launch {
        _broadcasts.value = Resource.Loading()
        _broadcasts.value = repo.getBroadcasts(keyword)
    }

    fun loadBroadcastDetail(id: Long) = viewModelScope.launch {
        _broadcastDetail.value = Resource.Loading()
        _broadcastDetail.value = repo.getBroadcastDetail(id)
    }

    fun loadAudienceCounts() = viewModelScope.launch {
        _audienceCounts.value = Resource.Loading()
        _audienceCounts.value = repo.getAudienceCounts()
    }

    fun createBroadcast(req: AdminBroadcastRequest, sendNow: Boolean) = viewModelScope.launch {
        _broadcastDetail.value = Resource.Loading()
        _broadcastDetail.value = repo.createBroadcast(req, sendNow)
    }

    fun updateBroadcast(id: Long, req: AdminBroadcastRequest) = viewModelScope.launch {
        _broadcastDetail.value = Resource.Loading()
        _broadcastDetail.value = repo.updateBroadcast(id, req)
    }

    fun sendBroadcast(id: Long) = viewModelScope.launch {
        _broadcastDetail.value = Resource.Loading()
        _broadcastDetail.value = repo.sendBroadcast(id)
    }

    fun changeAdminPassword(currentPassword: String, newPassword: String) = viewModelScope.launch {
        _passwordChange.value = Resource.Loading()
        _passwordChange.value = repo.changeAdminPassword(ChangePasswordRequest(currentPassword, newPassword))
    }

    fun resetPasswordChange() {
        _passwordChange.value = null
    }
}
