package com.example.rental_property_management_app.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rental_property_management_app.models.TenantInvoice
import com.example.rental_property_management_app.models.TenantInvoiceStatus
import com.example.rental_property_management_app.repositories.TenantPortalRepository
import com.example.rental_property_management_app.utils.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TenantInvoiceListState(
    val items: List<TenantInvoice> = emptyList(), val loading: Boolean = false,
    val error: String? = null, val nextPage: Int? = null
)

class TenantInvoiceViewModel : ViewModel() {
    private val repository = TenantPortalRepository()
    private val _list = MutableStateFlow(TenantInvoiceListState())
    val list = _list.asStateFlow()
    private val _detail = MutableStateFlow<Resource<TenantInvoice>>(Resource.Loading())
    val detail = _detail.asStateFlow()
    private var month: String? = null
    private var status: TenantInvoiceStatus? = null
    private var job: Job? = null

    fun filter(month: String?, status: TenantInvoiceStatus?) {
        this.month = month
        this.status = status
        refresh()
    }

    fun refresh() = load(0, emptyList())

    fun more() {
        val state = _list.value
        if (!state.loading) state.nextPage?.let { load(it, state.items) }
    }

    private fun load(page: Int, previous: List<TenantInvoice>) {
        job?.cancel()
        _list.value = TenantInvoiceListState(previous, loading = true)
        job = viewModelScope.launch {
            val result = repository.invoices(month, status, page)
            val data = result.data
            _list.value = if (data == null) {
                TenantInvoiceListState(previous, error = result.message, nextPage = page.takeIf { it > 0 })
            } else {
                TenantInvoiceListState((previous + data.content).distinctBy(TenantInvoice::id),
                    nextPage = (data.page + 1).takeIf { it < data.totalPages })
            }
        }
    }

    fun loadDetail(id: Long) {
        _detail.value = Resource.Loading()
        viewModelScope.launch { _detail.value = repository.invoice(id) }
    }
}
