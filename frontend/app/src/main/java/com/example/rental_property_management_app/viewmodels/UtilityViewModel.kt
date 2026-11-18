package com.example.rental_property_management_app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rental_property_management_app.models.*
import com.example.rental_property_management_app.repositories.RoomRepository
import com.example.rental_property_management_app.repositories.UtilityRepository
import com.example.rental_property_management_app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class UtilityViewModel : ViewModel() {

    private val utilityRepository = UtilityRepository()
    private val roomRepository = RoomRepository()

    private val _recordsState = MutableStateFlow<Resource<List<UtilityRecordResponse>>>(Resource.Loading())
    val recordsState: StateFlow<Resource<List<UtilityRecordResponse>>> = _recordsState.asStateFlow()

    private val _roomsState = MutableStateFlow<List<RoomResponse>>(emptyList())
    val roomsState: StateFlow<List<RoomResponse>> = _roomsState.asStateFlow()

    private val _previousIndexState = MutableStateFlow<PreviousUtilityIndexResponse?>(null)
    val previousIndexState: StateFlow<PreviousUtilityIndexResponse?> = _previousIndexState.asStateFlow()

    private val _saveState = MutableStateFlow<Resource<UtilityRecordResponse>?>(null)
    val saveState: StateFlow<Resource<UtilityRecordResponse>?> = _saveState.asStateFlow()

    fun loadRooms() {
        viewModelScope.launch {
            when (val result = roomRepository.getAllRooms(page = 0, size = 200)) {
                is Resource.Success -> _roomsState.value = result.data?.content ?: emptyList()
                else -> Unit
            }
        }
    }

    fun loadRecords(month: String? = null, roomId: Long? = null) {
        viewModelScope.launch {
            _recordsState.value = Resource.Loading()
            _recordsState.value = utilityRepository.getUtilities(month = month, roomId = roomId)
        }
    }

    fun loadPreviousIndex(roomId: Long, month: String?) {
        viewModelScope.launch {
            when (val res = utilityRepository.getPreviousIndex(roomId, month)) {
                is Resource.Success -> _previousIndexState.value = res.data
                else -> Unit
            }
        }
    }

    fun createUtility(request: CreateUtilityRequest) {
        viewModelScope.launch {
            _saveState.value = Resource.Loading()
            _saveState.value = utilityRepository.createUtility(request)
        }
    }

    fun updateUtility(id: Long, request: UpdateUtilityRequest) {
        viewModelScope.launch {
            _saveState.value = Resource.Loading()
            _saveState.value = utilityRepository.updateUtility(id, request)
        }
    }

    fun resetSaveState() {
        _saveState.value = null
    }
}
