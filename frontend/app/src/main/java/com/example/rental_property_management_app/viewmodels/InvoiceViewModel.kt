package com.example.rental_property_management_app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rental_property_management_app.models.*
import com.example.rental_property_management_app.repositories.InvoiceRepository
import com.example.rental_property_management_app.repositories.PropertyRepository
import com.example.rental_property_management_app.repositories.RoomRepository
import com.example.rental_property_management_app.repositories.TenantRepository
import com.example.rental_property_management_app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class InvoiceViewModel : ViewModel() {

    private val invoiceRepository = InvoiceRepository()
    private val roomRepository = RoomRepository()
    private val propertyRepository = PropertyRepository()
    private val tenantRepository = TenantRepository()

    private val _invoicesState = MutableStateFlow<Resource<List<InvoiceResponse>>>(Resource.Loading())
    val invoicesState: StateFlow<Resource<List<InvoiceResponse>>> = _invoicesState.asStateFlow()

    private val _roomsState = MutableStateFlow<List<RoomResponse>>(emptyList())
    val roomsState: StateFlow<List<RoomResponse>> = _roomsState.asStateFlow()

    private val _propertiesState = MutableStateFlow<List<PropertyResponse>>(emptyList())
    val propertiesState: StateFlow<List<PropertyResponse>> = _propertiesState.asStateFlow()

    private val _tenantsState = MutableStateFlow<List<TenantResponse>>(emptyList())
    val tenantsState: StateFlow<List<TenantResponse>> = _tenantsState.asStateFlow()

    private val _createInvoiceState = MutableStateFlow<Resource<InvoiceResponse>?>(null)
    val createInvoiceState: StateFlow<Resource<InvoiceResponse>?> = _createInvoiceState.asStateFlow()

    private val _batchCreateState = MutableStateFlow<Resource<BatchCreateInvoiceResult>?>(null)
    val batchCreateState: StateFlow<Resource<BatchCreateInvoiceResult>?> = _batchCreateState.asStateFlow()

    var currentMonthFilter: String? = null
    var currentRoomIdFilter: Long? = null
    var currentStatusFilter: InvoiceStatus? = null

    fun loadReferenceData() {
        viewModelScope.launch {
            when (val roomsResult = roomRepository.getAllRooms(page = 0, size = 200)) {
                is Resource.Success -> _roomsState.value = roomsResult.data?.content ?: emptyList()
                else -> Unit
            }
            when (val propResult = propertyRepository.getProperties(page = 0, size = 100)) {
                is Resource.Success -> _propertiesState.value = propResult.data?.content ?: emptyList()
                else -> Unit
            }
            when (val tenantResult = tenantRepository.getTenants(page = 0, size = 200)) {
                is Resource.Success -> _tenantsState.value = tenantResult.data?.content ?: emptyList()
                else -> Unit
            }
        }
    }

    fun loadInvoices(
        month: String? = currentMonthFilter,
        roomId: Long? = currentRoomIdFilter,
        status: InvoiceStatus? = currentStatusFilter
    ) {
        currentMonthFilter = month
        currentRoomIdFilter = roomId
        currentStatusFilter = status

        viewModelScope.launch {
            _invoicesState.value = Resource.Loading()
            _invoicesState.value = invoiceRepository.getInvoices(
                month = month,
                roomId = roomId,
                status = status
            )
        }
    }

    fun createInvoice(request: CreateInvoiceRequest) {
        viewModelScope.launch {
            _createInvoiceState.value = Resource.Loading()
            _createInvoiceState.value = invoiceRepository.createInvoice(request)
        }
    }

    fun batchCreateInvoices(request: BatchCreateInvoiceRequest) {
        viewModelScope.launch {
            _batchCreateState.value = Resource.Loading()
            _batchCreateState.value = invoiceRepository.batchCreateInvoices(request)
        }
    }

    fun resetCreateInvoiceState() {
        _createInvoiceState.value = null
    }

    fun resetBatchCreateState() {
        _batchCreateState.value = null
    }
}
