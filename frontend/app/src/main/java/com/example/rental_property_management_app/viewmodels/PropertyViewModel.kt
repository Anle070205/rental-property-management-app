package com.example.rental_property_management_app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rental_property_management_app.models.PropertyResponse
import com.example.rental_property_management_app.repositories.PropertyRepository
import com.example.rental_property_management_app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PropertyListState(
    val data: List<PropertyResponse>? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

class PropertyViewModel : ViewModel() {

    private val repository = PropertyRepository()

    private val _propertiesState = MutableStateFlow(PropertyListState())
    val propertiesState: StateFlow<PropertyListState> = _propertiesState.asStateFlow()

    fun loadProperties() {
        viewModelScope.launch {
            _propertiesState.value = PropertyListState(isLoading = true)

            when (val result = repository.getProperties()) {
                is Resource.Success -> {
                    _propertiesState.value = PropertyListState(data = result.data)
                }
                is Resource.Error -> {
                    _propertiesState.value = PropertyListState(error = result.message)
                }
                is Resource.Loading -> {
                    _propertiesState.value = PropertyListState(isLoading = true)
                }
            }
        }
    }
}
