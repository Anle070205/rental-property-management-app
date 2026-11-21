package com.example.rental_property_management_app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rental_property_management_app.models.*
import com.example.rental_property_management_app.repositories.IncidentRepository
import com.example.rental_property_management_app.utils.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class IncidentListState(
    val items: List<Incident> = emptyList(),
    val loading: Boolean = false,
    val error: String? = null,
    val nextPage: Int? = null
)

class IncidentViewModel : ViewModel() {
    private val repository = IncidentRepository()
    private val _list = MutableStateFlow(IncidentListState())
    val list = _list.asStateFlow()
    private val _detail = MutableStateFlow<Resource<Incident>>(Resource.Loading())
    val detail = _detail.asStateFlow()
    private val _save = MutableStateFlow<Resource<Incident>?>(null)
    val save = _save.asStateFlow()
    private var listJob: Job? = null
    private var filter: IncidentStatus? = null
    private var landlordMode: Boolean = false

    fun loadList(status: IncidentStatus?, isLandlord: Boolean = false) {
        filter = status
        landlordMode = isLandlord
        loadPage(0, emptyList())
    }

    fun more() {
        val state = _list.value
        if (!state.loading) state.nextPage?.let { loadPage(it, state.items) }
    }

    private fun loadPage(page: Int, previous: List<Incident>) {
        listJob?.cancel()
        _list.value = IncidentListState(previous, loading = true)
        listJob = viewModelScope.launch {
            val result = repository.list(filter, page, landlordMode)
            val data = result.data
            _list.value = if (data == null) {
                IncidentListState(previous, error = result.message, nextPage = page.takeIf { it > 0 })
            } else {
                IncidentListState(
                    (previous + data.content).distinctBy(Incident::id),
                    nextPage = (data.page + 1).takeIf { it < data.totalPages }
                )
            }
        }
    }

    fun loadDetail(id: Long, isLandlord: Boolean = false) {
        _detail.value = Resource.Loading()
        viewModelScope.launch { _detail.value = repository.get(id, isLandlord) }
    }

    fun create(request: CreateIncidentRequest) = submit { repository.create(request) }

    fun supplement(id: Long, request: UpdateIncidentRequest) = submit { repository.supplement(id, request) }

    fun updateLandlordStatus(id: Long, request: UpdateIncidentStatusRequest) =
        submit { repository.updateLandlordStatus(id, request) }

    fun clearSaveState() {
        _save.value = null
    }

    private fun submit(action: suspend () -> Resource<Incident>) {
        if (_save.value is Resource.Loading) return
        _save.value = Resource.Loading()
        viewModelScope.launch {
            val res = action()
            _save.value = res
            if (res is Resource.Success && res.data != null) {
                _detail.value = res
            }
        }
    }
}
