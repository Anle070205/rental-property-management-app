package com.example.rental_property_management_app.viewmodels

import android.graphics.Bitmap
import android.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.rental_property_management_app.models.TenantPayment
import com.example.rental_property_management_app.models.TenantPaymentInfo
import com.example.rental_property_management_app.repositories.TenantPortalRepository
import com.example.rental_property_management_app.utils.Resource
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.WriterException
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TenantPaymentQr(val info: TenantPaymentInfo, val image: Bitmap?)

data class TenantPaymentHistoryState(
    val items: List<TenantPayment> = emptyList(), val loading: Boolean = false,
    val error: String? = null, val nextPage: Int? = null
)

class TenantPaymentViewModel : ViewModel() {
    private val repository = TenantPortalRepository()
    private val _qr = MutableStateFlow<Resource<TenantPaymentQr>>(Resource.Loading())
    val qr = _qr.asStateFlow()
    private val _history = MutableStateFlow(TenantPaymentHistoryState())
    val history = _history.asStateFlow()
    private var historyJob: Job? = null

    fun loadQr(invoiceId: Long) {
        _qr.value = Resource.Loading()
        viewModelScope.launch {
            when (val result = repository.paymentInfo(invoiceId)) {
                is Resource.Success -> result.data?.let { info ->
                    val bitmap = withContext(Dispatchers.Default) { renderQr(info.qrPayload) }
                    _qr.value = Resource.Success(TenantPaymentQr(info, bitmap))
                }
                is Resource.Error -> _qr.value = Resource.Error(result.message.orEmpty())
                is Resource.Loading -> Unit
            }
        }
    }

    fun refreshHistory() = loadHistory(0, emptyList())

    fun moreHistory() {
        val state = _history.value
        if (!state.loading) state.nextPage?.let { loadHistory(it, state.items) }
    }

    private fun loadHistory(page: Int, previous: List<TenantPayment>) {
        historyJob?.cancel()
        _history.value = TenantPaymentHistoryState(previous, loading = true)
        historyJob = viewModelScope.launch {
            val result = repository.payments(page)
            val data = result.data
            _history.value = if (data == null) {
                TenantPaymentHistoryState(previous, error = result.message, nextPage = page.takeIf { it > 0 })
            } else {
                TenantPaymentHistoryState((previous + data.content).distinctBy(TenantPayment::id),
                    nextPage = (data.page + 1).takeIf { it < data.totalPages })
            }
        }
    }

    private fun renderQr(payload: String): Bitmap? {
        if (payload.isBlank()) return null
        val matrix = try {
            QRCodeWriter().encode(payload, BarcodeFormat.QR_CODE, 512, 512,
                mapOf(EncodeHintType.CHARACTER_SET to "UTF-8"))
        } catch (_: WriterException) {
            return null
        } catch (_: IllegalArgumentException) {
            return null
        }
        val pixels = IntArray(matrix.width * matrix.height) { index ->
            if (matrix[index % matrix.width, index / matrix.width]) Color.BLACK else Color.WHITE
        }
        return Bitmap.createBitmap(pixels, matrix.width, matrix.height, Bitmap.Config.ARGB_8888)
    }
}
