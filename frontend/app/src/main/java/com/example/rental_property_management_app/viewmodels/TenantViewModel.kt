package com.example.rental_property_management_app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rental_property_management_app.models.*
import com.example.rental_property_management_app.repositories.TenantRepository
import com.example.rental_property_management_app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * TenantViewModel
 * Quản lý UI state và business logic cho Tenant Management
 */
class TenantViewModel : ViewModel() {
    
    private val tenantRepository = TenantRepository()
    
    // StateFlows cho UI
    private val _tenantsState = MutableStateFlow<Resource<PageResponse<TenantResponse>>>(Resource.Loading())
    val tenantsState: StateFlow<Resource<PageResponse<TenantResponse>>> = _tenantsState.asStateFlow()
    
    data class TenantDetailState(
        val data: TenantResponse? = null,
        val isLoading: Boolean = false,
        val error: String? = null
    )
    
    data class CreateUpdateState(
        val data: TenantResponse? = null,
        val isLoading: Boolean = false,
        val error: String? = null
    )
    
    data class DeleteState(
        val data: Unit? = null,
        val isLoading: Boolean = false,
        val error: String? = null
    )
    
    private val _tenantDetailState = MutableStateFlow(TenantDetailState())
    val tenantDetailState: StateFlow<TenantDetailState> = _tenantDetailState.asStateFlow()
    
    private val _createUpdateState = MutableStateFlow(CreateUpdateState())
    val createUpdateState: StateFlow<CreateUpdateState> = _createUpdateState.asStateFlow()
    
    private val _deleteState = MutableStateFlow(DeleteState())
    val deleteState: StateFlow<DeleteState> = _deleteState.asStateFlow()

    private val _provisionState = MutableStateFlow<Resource<ProvisionTenantAccountResponse>?>(null)
    val provisionState: StateFlow<Resource<ProvisionTenantAccountResponse>?> = _provisionState.asStateFlow()
    
    // Filters
    private val _selectedPropertyId = MutableStateFlow<Long?>(null)
    val selectedPropertyId: StateFlow<Long?> = _selectedPropertyId.asStateFlow()
    
    private val _searchKeyword = MutableStateFlow<String?>(null)
    val searchKeyword: StateFlow<String?> = _searchKeyword.asStateFlow()
    
    /**
     * Load tenants với filters
     */
    fun loadTenants(
        propertyId: Long? = null,
        keyword: String? = null,
        page: Int = 0,
        size: Int = 10,
        sortBy: String = "fullName",
        sortDir: String = "asc"
    ) {
        viewModelScope.launch {
            _tenantsState.value = Resource.Loading()
            _tenantsState.value = tenantRepository.getTenants(propertyId, keyword, page, size, sortBy, sortDir)
        }
    }
    
    /**
     * Load tenant detail
     */
    fun loadTenantDetail(id: Long) {
        viewModelScope.launch {
            _tenantDetailState.value = TenantDetailState(isLoading = true)
            when (val result = tenantRepository.getTenantById(id)) {
                is Resource.Success -> {
                    _tenantDetailState.value = TenantDetailState(data = result.data)
                }
                is Resource.Error -> {
                    _tenantDetailState.value = TenantDetailState(error = result.message)
                }
                is Resource.Loading -> {
                    _tenantDetailState.value = TenantDetailState(isLoading = true)
                }
            }
        }
    }
    
    /**
     * Get tenant detail (alias for TenantFormFragment)
     */
    fun getTenantDetail(id: Long) = loadTenantDetail(id)
    
    /**
     * Create tenant
     */
    fun createTenant(request: CreateTenantRequest) {
        viewModelScope.launch {
            _createUpdateState.value = CreateUpdateState(isLoading = true)
            when (val result = tenantRepository.createTenant(request)) {
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
     * Update tenant
     */
    fun updateTenant(id: Long, request: UpdateTenantRequest) {
        viewModelScope.launch {
            _createUpdateState.value = CreateUpdateState(isLoading = true)
            when (val result = tenantRepository.updateTenant(id, request)) {
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
     * Delete tenant
     */
    fun deleteTenant(id: Long) {
        viewModelScope.launch {
            _deleteState.value = DeleteState(isLoading = true)
            when (val result = tenantRepository.deleteTenant(id)) {
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
     * Search tenants
     */
    fun searchTenants(keyword: String) {
        _searchKeyword.value = keyword
        loadTenants(keyword = keyword)
    }
    
    /**
     * Filter by property
     */
    fun filterByProperty(propertyId: Long?) {
        _selectedPropertyId.value = propertyId
        loadTenants(propertyId = propertyId, keyword = _searchKeyword.value)
    }
    
    /**
     * Clear filters
     */
    fun clearFilters() {
        _selectedPropertyId.value = null
        _searchKeyword.value = null
        loadTenants()
    }
    
    /**
     * Reset states
     */
    fun resetDetailState() {
        _tenantDetailState.value = TenantDetailState()
    }
    
    fun resetCreateUpdateState() {
        _createUpdateState.value = CreateUpdateState()
    }
    
    fun resetDeleteState() {
        _deleteState.value = DeleteState()
    }

    fun provisionTenantAccount(id: Long, request: ProvisionTenantAccountRequest) {
        viewModelScope.launch {
            _provisionState.value = Resource.Loading()
            val res = tenantRepository.provisionTenantAccount(id, request)
            _provisionState.value = res
            if (res is Resource.Success) {
                loadTenantDetail(id)
            }
        }
    }

    fun resendTenantActivation(id: Long, channel: String) {
        viewModelScope.launch {
            _provisionState.value = Resource.Loading()
            val res = tenantRepository.resendTenantActivation(id, channel)
            _provisionState.value = res
        }
    }

    fun resetProvisionState() {
        _provisionState.value = null
    }
}
