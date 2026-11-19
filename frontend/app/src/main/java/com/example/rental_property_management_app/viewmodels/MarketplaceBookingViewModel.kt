package com.example.rental_property_management_app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rental_property_management_app.models.*
import com.example.rental_property_management_app.repositories.MarketplaceBookingRepository
import com.example.rental_property_management_app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MarketplaceBookingViewModel : ViewModel() {

    private val repository = MarketplaceBookingRepository()

    private val _propertiesState = MutableStateFlow<Resource<List<MarketplaceProperty>>>(Resource.Loading())
    val propertiesState: StateFlow<Resource<List<MarketplaceProperty>>> = _propertiesState.asStateFlow()

    private val _roomsState = MutableStateFlow<Resource<List<MarketplaceRoom>>>(Resource.Loading())
    val roomsState: StateFlow<Resource<List<MarketplaceRoom>>> = _roomsState.asStateFlow()

    private val _roomDetailState = MutableStateFlow<Resource<MarketplaceRoom>>(Resource.Loading())
    val roomDetailState: StateFlow<Resource<MarketplaceRoom>> = _roomDetailState.asStateFlow()

    private val _bookingsState = MutableStateFlow<Resource<List<BookingRequestItem>>>(Resource.Loading())
    val bookingsState: StateFlow<Resource<List<BookingRequestItem>>> = _bookingsState.asStateFlow()

    private val _actionState = MutableStateFlow<Resource<BookingRequestItem>?>(null)
    val actionState: StateFlow<Resource<BookingRequestItem>?> = _actionState.asStateFlow()

    fun loadProperties(keyword: String? = null, onlyVacant: Boolean? = null) {
        viewModelScope.launch {
            _propertiesState.value = Resource.Loading()
            _propertiesState.value = repository.browseProperties(keyword, onlyVacant)
        }
    }

    fun loadPropertyRooms(propertyId: Long, status: RoomStatus? = null, keyword: String? = null) {
        viewModelScope.launch {
            _roomsState.value = Resource.Loading()
            _roomsState.value = repository.browsePropertyRooms(propertyId, status, keyword)
        }
    }

    fun loadRoomDetail(roomId: Long) {
        viewModelScope.launch {
            _roomDetailState.value = Resource.Loading()
            _roomDetailState.value = repository.getMarketplaceRoomDetail(roomId)
        }
    }

    fun submitBookingOrContact(request: CreateBookingRequest) {
        viewModelScope.launch {
            _actionState.value = Resource.Loading()
            _actionState.value = repository.createBookingOrContact(request)
        }
    }

    fun loadTenantBookings() {
        viewModelScope.launch {
            _bookingsState.value = Resource.Loading()
            _bookingsState.value = repository.getTenantBookings()
        }
    }

    fun cancelTenantBooking(id: Long) {
        viewModelScope.launch {
            _actionState.value = Resource.Loading()
            _actionState.value = repository.cancelTenantBooking(id)
        }
    }

    fun loadLandlordBookings(
        status: BookingRequestStatus? = null,
        requestType: BookingRequestType? = null
    ) {
        viewModelScope.launch {
            _bookingsState.value = Resource.Loading()
            _bookingsState.value = repository.getLandlordBookings(status, requestType)
        }
    }

    fun respondLandlordBooking(id: Long, status: BookingRequestStatus, landlordNote: String?) {
        viewModelScope.launch {
            _actionState.value = Resource.Loading()
            _actionState.value = repository.respondLandlordBooking(
                id,
                RespondBookingRequest(status = status, landlordNote = landlordNote)
            )
        }
    }

    fun consumeActionState() {
        _actionState.value = null
    }
}
