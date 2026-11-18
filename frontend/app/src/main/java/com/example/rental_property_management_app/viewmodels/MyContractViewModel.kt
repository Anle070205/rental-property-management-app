package com.example.rental_property_management_app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rental_property_management_app.models.MyContract
import com.example.rental_property_management_app.repositories.TenantPortalRepository
import com.example.rental_property_management_app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MyContractViewModel : ViewModel() {
    private val repository = TenantPortalRepository()
    private val _contract = MutableStateFlow<Resource<MyContract>>(Resource.Loading())
    val contract = _contract.asStateFlow()
    private val _renewal = MutableStateFlow<Resource<Unit>?>(null)
    val renewal = _renewal.asStateFlow()

    init { load() }

    fun load() {
        _contract.value = Resource.Loading()
        viewModelScope.launch { _contract.value = repository.contract() }
    }

    fun requestRenewal(id: Long) {
        if (_renewal.value is Resource.Loading || _renewal.value is Resource.Success) return
        _renewal.value = Resource.Loading()
        viewModelScope.launch { _renewal.value = repository.requestRenewal(id) }
    }

    fun clearRenewalError() {
        if (_renewal.value is Resource.Error) _renewal.value = null
    }
}
