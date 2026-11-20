package com.example.rental_property_management_app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rental_property_management_app.models.CreateGuestStayRequest
import com.example.rental_property_management_app.models.GuestStayResponse
import com.example.rental_property_management_app.repositories.GuestStayRepository
import com.example.rental_property_management_app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class GuestStayViewModel(
    private val repository: GuestStayRepository = GuestStayRepository()
) : ViewModel() {

    private val _listState = MutableStateFlow<Resource<List<GuestStayResponse>>?>(null)
    val listState: StateFlow<Resource<List<GuestStayResponse>>?> = _listState

    private val _detailState = MutableStateFlow<Resource<GuestStayResponse>?>(null)
    val detailState: StateFlow<Resource<GuestStayResponse>?> = _detailState

    private val _actionState = MutableStateFlow<Resource<GuestStayResponse>?>(null)
    val actionState: StateFlow<Resource<GuestStayResponse>?> = _actionState

    private val _deleteState = MutableStateFlow<Resource<Unit>?>(null)
    val deleteState: StateFlow<Resource<Unit>?> = _deleteState

    fun loadList(isLandlord: Boolean, status: String? = null) {
        viewModelScope.launch {
            _listState.value = Resource.Loading()
            _listState.value = if (isLandlord) {
                repository.getLandlordGuestStays(status)
            } else {
                repository.getTenantGuestStays(status)
            }
        }
    }

    fun loadDetail(isLandlord: Boolean, id: Long) {
        viewModelScope.launch {
            _detailState.value = Resource.Loading()
            _detailState.value = if (isLandlord) {
                repository.getLandlordGuestStayDetail(id)
            } else {
                repository.getTenantGuestStayDetail(id)
            }
        }
    }

    fun createGuestStay(request: CreateGuestStayRequest) {
        viewModelScope.launch {
            _actionState.value = Resource.Loading()
            _actionState.value = repository.createGuestStay(request)
        }
    }

    fun cancelGuestStay(id: Long) {
        viewModelScope.launch {
            _actionState.value = Resource.Loading()
            _actionState.value = repository.cancelGuestStay(id)
        }
    }

    fun deleteGuestStay(id: Long) {
        viewModelScope.launch {
            _deleteState.value = Resource.Loading()
            _deleteState.value = repository.deleteGuestStay(id)
        }
    }

    fun updateStatus(id: Long, status: String, landlordNote: String?) {
        viewModelScope.launch {
            _actionState.value = Resource.Loading()
            _actionState.value = repository.updateGuestStayStatus(id, status, landlordNote)
        }
    }

    fun resetActionState() {
        _actionState.value = null
    }

    fun resetDeleteState() {
        _deleteState.value = null
    }
}
