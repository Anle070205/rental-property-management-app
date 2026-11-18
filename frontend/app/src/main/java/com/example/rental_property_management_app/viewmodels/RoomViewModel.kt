package com.example.rental_property_management_app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rental_property_management_app.models.*
import com.example.rental_property_management_app.repositories.PropertyRepository
import com.example.rental_property_management_app.repositories.RoomRepository
import com.example.rental_property_management_app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * RoomViewModel
 * Quản lý UI state và business logic cho Room Management
 */
class RoomViewModel : ViewModel() {
    
    private val roomRepository = RoomRepository()
    private val propertyRepository = PropertyRepository()
    
    // StateFlows cho UI
    private val _roomsState = MutableStateFlow<Resource<PageResponse<RoomResponse>>>(Resource.Loading())
    val roomsState: StateFlow<Resource<PageResponse<RoomResponse>>> = _roomsState.asStateFlow()
    
    data class RoomDetailState(
        val data: RoomResponse? = null,
        val isLoading: Boolean = false,
        val error: String? = null
    )
    
    private val _roomDetailState = MutableStateFlow(RoomDetailState())
    val roomDetailState: StateFlow<RoomDetailState> = _roomDetailState.asStateFlow()
    
    data class CreateUpdateState(
        val data: RoomResponse? = null,
        val isLoading: Boolean = false,
        val error: String? = null
    )
    
    private val _createUpdateState = MutableStateFlow(CreateUpdateState())
    val createUpdateState: StateFlow<CreateUpdateState> = _createUpdateState.asStateFlow()
    
    data class DeleteState(
        val data: Unit? = null,
        val isLoading: Boolean = false,
        val error: String? = null
    )
    
    private val _deleteState = MutableStateFlow(DeleteState())
    val deleteState: StateFlow<DeleteState> = _deleteState.asStateFlow()
    
    private val _propertiesState = MutableStateFlow<Resource<PageResponse<PropertyResponse>>>(Resource.Loading())
    val propertiesState: StateFlow<Resource<PageResponse<PropertyResponse>>> = _propertiesState.asStateFlow()
    
    // Filters
    private val _selectedPropertyId = MutableStateFlow<Long?>(null)
    val selectedPropertyId: StateFlow<Long?> = _selectedPropertyId.asStateFlow()
    
    private val _selectedStatus = MutableStateFlow<RoomStatus?>(null)
    val selectedStatus: StateFlow<RoomStatus?> = _selectedStatus.asStateFlow()
    
    private val _selectedFloor = MutableStateFlow<Int?>(null)
    val selectedFloor: StateFlow<Int?> = _selectedFloor.asStateFlow()
    
    /**
     * Load all rooms
     */
    fun loadAllRooms(
        page: Int = 0,
        size: Int = 20,
        sortBy: String = "roomCode",
        sortDir: String = "asc"
    ) {
        viewModelScope.launch {
            _roomsState.value = Resource.Loading()
            _roomsState.value = roomRepository.getAllRooms(page, size, sortBy, sortDir)
        }
    }
    
    /**
     * Load rooms by property với filters
     */
    fun loadRoomsByProperty(
        propertyId: Long,
        status: RoomStatus? = null,
        minPrice: String? = null,
        maxPrice: String? = null,
        floor: Int? = null,
        page: Int = 0,
        size: Int = 20
    ) {
        viewModelScope.launch {
            _roomsState.value = Resource.Loading()
            _roomsState.value = roomRepository.getRoomsByProperty(
                propertyId, status, minPrice, maxPrice, floor, page, size
            )
        }
    }
    
    /**
     * Load room detail
     */
    fun loadRoomDetail(id: Long) {
        viewModelScope.launch {
            _roomDetailState.value = RoomDetailState(isLoading = true)
            when (val result = roomRepository.getRoomById(id)) {
                is Resource.Success -> {
                    _roomDetailState.value = RoomDetailState(data = result.data)
                }
                is Resource.Error -> {
                    _roomDetailState.value = RoomDetailState(error = result.message)
                }
                is Resource.Loading -> {
                    _roomDetailState.value = RoomDetailState(isLoading = true)
                }
            }
        }
    }
    
    /**
     * Get room detail (alias for forms)
     */
    fun getRoomDetail(id: Long) = loadRoomDetail(id)
    
    /**
     * Create new room
     */
    fun createRoom(request: CreateRoomRequest) {
        viewModelScope.launch {
            _createUpdateState.value = CreateUpdateState(isLoading = true)
            when (val result = roomRepository.createRoom(request)) {
                is Resource.Success -> {
                    _createUpdateState.value = CreateUpdateState(data = result.data)
                }
                is Resource.Error -> {
                    _createUpdateState.value = CreateUpdateState(error = result.message)
                }
                is Resource.Loading -> {
                    _createUpdateState.value = CreateUpdateState(isLoading = true)
                }
            }
        }
    }
    
    /**
     * Update room
     */
    fun updateRoom(id: Long, request: UpdateRoomRequest) {
        viewModelScope.launch {
            _createUpdateState.value = CreateUpdateState(isLoading = true)
            when (val result = roomRepository.updateRoom(id, request)) {
                is Resource.Success -> {
                    _createUpdateState.value = CreateUpdateState(data = result.data)
                }
                is Resource.Error -> {
                    _createUpdateState.value = CreateUpdateState(error = result.message)
                }
                is Resource.Loading -> {
                    _createUpdateState.value = CreateUpdateState(isLoading = true)
                }
            }
        }
    }
    
    /**
     * Delete room
     */
    fun deleteRoom(id: Long) {
        viewModelScope.launch {
            _deleteState.value = DeleteState(isLoading = true)
            when (val result = roomRepository.deleteRoom(id)) {
                is Resource.Success -> {
                    _deleteState.value = DeleteState(data = Unit)
                }
                is Resource.Error -> {
                    _deleteState.value = DeleteState(error = result.message)
                }
                is Resource.Loading -> {
                    _deleteState.value = DeleteState(isLoading = true)
                }
            }
        }
    }
    
    /**
     * Load properties for selection
     */
    fun loadProperties() {
        viewModelScope.launch {
            _propertiesState.value = Resource.Loading()
            _propertiesState.value = propertyRepository.getProperties(page = 0, size = 100)
        }
    }
    
    /**
     * Update filters
     */
    fun updateFilters(propertyId: Long? = null, status: RoomStatus? = null, floor: Int? = null) {
        _selectedPropertyId.value = propertyId
        _selectedStatus.value = status
        _selectedFloor.value = floor
    }
    
    /**
     * Clear filters
     */
    fun clearFilters() {
        _selectedPropertyId.value = null
        _selectedStatus.value = null
        _selectedFloor.value = null
    }
    
    /**
     * Reset states
     */
    fun resetCreateUpdateState() {
        _createUpdateState.value = CreateUpdateState()
    }
    
    fun resetDeleteState() {
        _deleteState.value = DeleteState()
    }
    
    fun resetDetailState() {
        _roomDetailState.value = RoomDetailState()
    }
}
