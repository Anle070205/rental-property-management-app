package com.example.rental_property_management_app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rental_property_management_app.models.HealthScoreOverviewResponse
import com.example.rental_property_management_app.models.RoomHealthScoreResponse
import com.example.rental_property_management_app.repositories.HealthScoreRepository
import com.example.rental_property_management_app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HealthScoreViewModel : ViewModel() {

    private val repository = HealthScoreRepository()

    private val _overviewState = MutableStateFlow<Resource<HealthScoreOverviewResponse>>(Resource.Loading())
    val overviewState: StateFlow<Resource<HealthScoreOverviewResponse>> = _overviewState.asStateFlow()

    private val _roomScoreState = MutableStateFlow<Resource<RoomHealthScoreResponse>?>(null)
    val roomScoreState: StateFlow<Resource<RoomHealthScoreResponse>?> = _roomScoreState.asStateFlow()

    var currentRatingLevel: String? = null

    fun loadHealthScores(ratingLevel: String? = currentRatingLevel) {
        currentRatingLevel = ratingLevel
        viewModelScope.launch {
            _overviewState.value = Resource.Loading()
            _overviewState.value = repository.getHealthScores(ratingLevel = ratingLevel)
        }
    }

    fun loadRoomHealthScore(roomId: Long) {
        viewModelScope.launch {
            _roomScoreState.value = Resource.Loading()
            _roomScoreState.value = repository.getRoomHealthScore(roomId)
        }
    }

    fun resetRoomScoreState() {
        _roomScoreState.value = null
    }
}
