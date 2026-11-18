package com.example.rental_property_management_app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rental_property_management_app.models.DebtSummaryResponse
import com.example.rental_property_management_app.models.RoomResponse
import com.example.rental_property_management_app.models.RoomTenantDebtResponse
import com.example.rental_property_management_app.repositories.DebtRepository
import com.example.rental_property_management_app.repositories.RoomRepository
import com.example.rental_property_management_app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DebtViewModel : ViewModel() {

    private val debtRepository = DebtRepository()
    private val roomRepository = RoomRepository()

    private val _debtsState = MutableStateFlow<Resource<DebtSummaryResponse>>(Resource.Loading())
    val debtsState: StateFlow<Resource<DebtSummaryResponse>> = _debtsState.asStateFlow()

    private val _tenantDebtDetailState = MutableStateFlow<Resource<RoomTenantDebtResponse>?>(null)
    val tenantDebtDetailState: StateFlow<Resource<RoomTenantDebtResponse>?> = _tenantDebtDetailState.asStateFlow()

    private val _roomsState = MutableStateFlow<List<RoomResponse>>(emptyList())
    val roomsState: StateFlow<List<RoomResponse>> = _roomsState.asStateFlow()

    var currentRoomId: Long? = null
    var currentMonth: String? = null
    var currentOverdueLevel: String? = null
    var currentKeyword: String? = null

    fun loadRooms() {
        viewModelScope.launch {
            when (val res = roomRepository.getAllRooms(page = 0, size = 200)) {
                is Resource.Success -> _roomsState.value = res.data?.content ?: emptyList()
                else -> Unit
            }
        }
    }

    fun loadDebts(
        roomId: Long? = currentRoomId,
        month: String? = currentMonth,
        overdueLevel: String? = currentOverdueLevel,
        keyword: String? = currentKeyword
    ) {
        currentRoomId = roomId
        currentMonth = month
        currentOverdueLevel = overdueLevel
        currentKeyword = keyword

        viewModelScope.launch {
            _debtsState.value = Resource.Loading()
            _debtsState.value = debtRepository.getDebts(
                roomId = roomId,
                month = month,
                overdueLevel = overdueLevel,
                keyword = keyword?.ifBlank { null }
            )
        }
    }

    fun loadTenantDebtDetail(tenantId: Long) {
        viewModelScope.launch {
            _tenantDebtDetailState.value = Resource.Loading()
            _tenantDebtDetailState.value = debtRepository.getTenantDebtDetail(tenantId)
        }
    }

    fun resetTenantDebtDetailState() {
        _tenantDebtDetailState.value = null
    }
}
