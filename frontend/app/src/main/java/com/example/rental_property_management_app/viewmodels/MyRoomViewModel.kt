package com.example.rental_property_management_app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rental_property_management_app.models.MyRoom
import com.example.rental_property_management_app.repositories.TenantPortalRepository
import com.example.rental_property_management_app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MyRoomViewModel : ViewModel() {
    private val repository = TenantPortalRepository()
    private val _room = MutableStateFlow<Resource<MyRoom>>(Resource.Loading())
    val room = _room.asStateFlow()

    init { load() }

    fun load() {
        _room.value = Resource.Loading()
        viewModelScope.launch { _room.value = repository.room() }
    }
}
