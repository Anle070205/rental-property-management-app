package com.example.rental_property_management_app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rental_property_management_app.models.CreatePropertyRequest
import com.example.rental_property_management_app.models.PropertyResponse
import com.example.rental_property_management_app.models.UpdatePropertyRequest
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

    private val _saveState = MutableStateFlow<Resource<PropertyResponse>?>(null)
    val saveState: StateFlow<Resource<PropertyResponse>?> = _saveState.asStateFlow()

    fun loadProperties() {
        viewModelScope.launch {
            _propertiesState.value = PropertyListState(isLoading = true)

            when (val result = repository.getProperties(page = 0, size = 100)) {
                is Resource.Success -> {
                    _propertiesState.value = PropertyListState(data = result.data?.content.orEmpty())
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

    fun createProperty(name: String, address: String, description: String?) {
        viewModelScope.launch {
            _saveState.value = Resource.Loading()
            val result = repository.createProperty(
                CreatePropertyRequest(name = name, address = address, description = description)
            )
            _saveState.value = result
            if (result is Resource.Success) {
                loadProperties()
            }
        }
    }

    fun updateProperty(id: Long, name: String, address: String, description: String?) {
        viewModelScope.launch {
            _saveState.value = Resource.Loading()
            val result = repository.updateProperty(
                id,
                UpdatePropertyRequest(name = name, address = address, description = description)
            )
            _saveState.value = result
            if (result is Resource.Success) {
                loadProperties()
            }
        }
    }

    fun consumeSaveState() {
        _saveState.value = null
    }
}
