package com.example.rental_property_management_app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rental_property_management_app.models.*
import com.example.rental_property_management_app.repositories.ContractRepository
import com.example.rental_property_management_app.repositories.RoomRepository
import com.example.rental_property_management_app.repositories.TenantRepository
import com.example.rental_property_management_app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContractViewModel : ViewModel() {

    private val contractRepository = ContractRepository()
    private val roomRepository = RoomRepository()
    private val tenantRepository = TenantRepository()

    private val _contractsState = MutableStateFlow<Resource<List<ContractResponse>>>(Resource.Loading())
    val contractsState: StateFlow<Resource<List<ContractResponse>>> = _contractsState.asStateFlow()

    private val _contractDetailState = MutableStateFlow<Resource<ContractResponse>>(Resource.Loading())
    val contractDetailState: StateFlow<Resource<ContractResponse>> = _contractDetailState.asStateFlow()

    private val _actionState = MutableStateFlow<Resource<ContractResponse>?>(null)
    val actionState: StateFlow<Resource<ContractResponse>?> = _actionState.asStateFlow()

    private val _roommateActionState = MutableStateFlow<Resource<String>?>(null)
    val roommateActionState: StateFlow<Resource<String>?> = _roommateActionState.asStateFlow()

    private val _roomsState = MutableStateFlow<List<RoomResponse>>(emptyList())
    val roomsState: StateFlow<List<RoomResponse>> = _roomsState.asStateFlow()

    private val _tenantsState = MutableStateFlow<List<TenantResponse>>(emptyList())
    val tenantsState: StateFlow<List<TenantResponse>> = _tenantsState.asStateFlow()

    var currentStatusFilter: ContractStatus? = null
    var currentRoomIdFilter: Long? = null
    var currentTenantIdFilter: Long? = null

    fun loadReferenceData() {
        viewModelScope.launch {
            val roomsRes = roomRepository.getAllRooms(page = 0, size = 200)
            if (roomsRes is Resource.Success) {
                _roomsState.value = roomsRes.data?.content ?: emptyList()
            }
            val tenantsRes = tenantRepository.getTenants(page = 0, size = 200)
            if (tenantsRes is Resource.Success) {
                _tenantsState.value = tenantsRes.data?.content ?: emptyList()
            }
        }
    }

    fun loadContracts(
        roomId: Long? = currentRoomIdFilter,
        tenantId: Long? = currentTenantIdFilter,
        status: ContractStatus? = currentStatusFilter
    ) {
        currentRoomIdFilter = roomId
        currentTenantIdFilter = tenantId
        currentStatusFilter = status
        viewModelScope.launch {
            _contractsState.value = Resource.Loading()
            _contractsState.value = contractRepository.getContracts(
                roomId = roomId,
                tenantId = tenantId,
                status = status
            )
        }
    }

    fun loadContractDetail(contractId: Long) {
        viewModelScope.launch {
            _contractDetailState.value = Resource.Loading()
            _contractDetailState.value = contractRepository.getContractById(contractId)
        }
    }

    fun createContract(request: CreateContractRequest) {
        viewModelScope.launch {
            _actionState.value = Resource.Loading()
            val res = contractRepository.createContract(request)
            _actionState.value = res
            if (res is Resource.Success) {
                loadContracts()
            }
        }
    }

    fun signContract(contractId: Long) {
        viewModelScope.launch {
            _actionState.value = Resource.Loading()
            val res = contractRepository.signContract(contractId)
            _actionState.value = res
            if (res is Resource.Success) {
                _contractDetailState.value = res
            }
        }
    }

    fun activateContract(contractId: Long) {
        viewModelScope.launch {
            _actionState.value = Resource.Loading()
            val res = contractRepository.activateContract(contractId)
            _actionState.value = res
            if (res is Resource.Success) {
                _contractDetailState.value = res
            }
        }
    }

    fun terminateContract(contractId: Long, reason: String? = null) {
        viewModelScope.launch {
            _actionState.value = Resource.Loading()
            val res = contractRepository.terminateContract(contractId, reason)
            _actionState.value = res
            if (res is Resource.Success) {
                _contractDetailState.value = res
            }
        }
    }

    fun addRoommate(request: CreateRoommateRequest, contractId: Long) {
        viewModelScope.launch {
            _roommateActionState.value = Resource.Loading()
            when (val res = contractRepository.addRoommate(request)) {
                is Resource.Success -> {
                    _roommateActionState.value = Resource.Success("Đã thêm người ở cùng")
                    loadContractDetail(contractId)
                }
                is Resource.Error -> _roommateActionState.value = Resource.Error(res.message ?: "Không thể thêm người ở cùng")
                is Resource.Loading -> Unit
            }
        }
    }

    fun deleteRoommate(roommateId: Long, contractId: Long) {
        viewModelScope.launch {
            _roommateActionState.value = Resource.Loading()
            when (val res = contractRepository.deleteRoommate(roommateId)) {
                is Resource.Success -> {
                    _roommateActionState.value = Resource.Success("Đã xóa người ở cùng")
                    loadContractDetail(contractId)
                }
                is Resource.Error -> _roommateActionState.value = Resource.Error(res.message ?: "Không thể xóa người ở cùng")
                is Resource.Loading -> Unit
            }
        }
    }

    fun resetActionState() {
        _actionState.value = null
    }

    fun resetRoommateActionState() {
        _roommateActionState.value = null
    }
}
