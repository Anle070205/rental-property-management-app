package com.example.rental_property_management_app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rental_property_management_app.models.LandlordDashboardResponse
import com.example.rental_property_management_app.repositories.DashboardRepository
import com.example.rental_property_management_app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DashboardViewModel : ViewModel() {

    private val repository = DashboardRepository()

    private val _dashboardState = MutableStateFlow<Resource<LandlordDashboardResponse>>(Resource.Loading())
    val dashboardState: StateFlow<Resource<LandlordDashboardResponse>> = _dashboardState.asStateFlow()

    fun loadDashboard() {
        viewModelScope.launch {
            _dashboardState.value = Resource.Loading()
            _dashboardState.value = repository.getLandlordDashboard()
        }
    }
}
