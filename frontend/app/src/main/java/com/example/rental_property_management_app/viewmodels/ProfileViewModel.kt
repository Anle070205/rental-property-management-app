package com.example.rental_property_management_app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rental_property_management_app.models.*
import com.example.rental_property_management_app.repositories.UserRepository
import com.example.rental_property_management_app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val repository = UserRepository()
    private val _profile = MutableStateFlow<Resource<ProfileResponse>>(Resource.Loading())
    val profile = _profile.asStateFlow()
    private val _mutation = MutableStateFlow<Resource<Unit>?>(null)
    val mutation = _mutation.asStateFlow()

    init {
        load()
    }

    fun load() = viewModelScope.launch {
        _profile.value = Resource.Loading(_profile.value.data)
        _profile.value = repository.profile()
    }

    fun save(request: UpdateProfileRequest) = mutate {
        when (val result = repository.update(request)) {
            is Resource.Success -> {
                _profile.value = result
                Resource.Success(Unit)
            }
            is Resource.Error -> Resource.Error(result.message.orEmpty())
            is Resource.Loading -> Resource.Loading()
        }
    }

    fun changePassword(request: ChangePasswordRequest) = mutate {
        repository.changePassword(request)
    }

    fun requestContactOtp(channel: String, target: String, onResult: (Resource<Unit>) -> Unit) {
        viewModelScope.launch {
            onResult(repository.requestContactOtp(RequestContactOtpRequest(channel, target)))
        }
    }

    fun verifyContactOtp(channel: String, target: String, otp: String, onResult: (Resource<ProfileResponse>) -> Unit) {
        viewModelScope.launch {
            val res = repository.verifyContactOtp(VerifyContactOtpRequest(channel, target, otp))
            if (res is Resource.Success) {
                _profile.value = res
            }
            onResult(res)
        }
    }

    fun consumeMutation() {
        _mutation.value = null
    }

    private fun mutate(action: suspend () -> Resource<Unit>) {
        if (_mutation.value is Resource.Loading) return
        _mutation.value = Resource.Loading()
        viewModelScope.launch { _mutation.value = action() }
    }
}
