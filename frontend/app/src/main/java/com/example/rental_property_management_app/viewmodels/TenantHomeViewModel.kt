package com.example.rental_property_management_app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rental_property_management_app.models.TenantDashboard
import com.example.rental_property_management_app.repositories.TenantPortalRepository
import com.example.rental_property_management_app.utils.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TenantHomeViewModel : ViewModel() {
    private val repository = TenantPortalRepository()
    private val _dashboard = MutableStateFlow<Resource<TenantDashboard>>(Resource.Loading())
    val dashboard = _dashboard.asStateFlow()
    private var load: Job? = null

    fun refresh() {
        load?.cancel()
        _dashboard.value = Resource.Loading(_dashboard.value.data)
        load = viewModelScope.launch { _dashboard.value = repository.dashboard() }
    }
}
