package com.example.rental_property_management_app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rental_property_management_app.models.NotificationItem
import com.example.rental_property_management_app.models.NotificationType
import com.example.rental_property_management_app.repositories.NotificationRepository
import com.example.rental_property_management_app.utils.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class NotificationState(
    val items: List<NotificationItem> = emptyList(),
    val page: Int = 0,
    val hasMore: Boolean = false,
    val loading: Boolean = false,
    val error: String? = null
)

class NotificationViewModel : ViewModel() {
    private val repository = NotificationRepository()
    private val _state = MutableStateFlow(NotificationState())
    val state = _state.asStateFlow()
    private val _opened = MutableStateFlow<NotificationItem?>(null)
    val opened = _opened.asStateFlow()
    private var type: NotificationType? = null
    private var unreadOnly = false
    private var loadJob: kotlinx.coroutines.Job? = null

    fun filter(type: NotificationType?, unreadOnly: Boolean) {
        this.type = type
        this.unreadOnly = unreadOnly
        refresh()
    }

    fun refresh() = load(0)

    fun loadMore() {
        val current = _state.value
        if (!current.loading && current.hasMore) load(current.page + 1)
    }

    fun markRead(item: NotificationItem) {
        if (item.isRead) {
            _opened.value = item
        } else {
            mutate {
                repository.markRead(item.id).also { result ->
                    if (result is Resource.Success) _opened.value = item
                }
            }
        }
    }

    fun consumeOpened() {
        _opened.value = null
    }

    fun markAllRead() = mutate { repository.markAllRead() }

    private fun mutate(action: suspend () -> Resource<Unit>) {
        if (_state.value.loading) return
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            when (val result = action()) {
                is Resource.Success -> refresh()
                else -> _state.value = _state.value.copy(loading = false, error = result.message)
            }
        }
    }

    private fun load(page: Int) {
        loadJob?.cancel()
        val previous = if (page == 0) NotificationState() else _state.value
        _state.value = previous.copy(loading = true, error = null)
        loadJob = viewModelScope.launch {
            when (val result = repository.list(type, unreadOnly, page)) {
                is Resource.Success -> result.data?.let {
                    _state.value = NotificationState(
                        items = (previous.items + it.content).distinctBy(NotificationItem::id),
                        page = it.page,
                        hasMore = it.page + 1 < it.totalPages
                    )
                }
                else -> _state.value = previous.copy(error = result.message)
            }
        }
    }
}
