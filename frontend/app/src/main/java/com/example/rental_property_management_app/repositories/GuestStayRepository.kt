package com.example.rental_property_management_app.repositories

import com.example.rental_property_management_app.models.CreateGuestStayRequest
import com.example.rental_property_management_app.models.GuestStayResponse
import com.example.rental_property_management_app.models.UpdateGuestStayStatusRequest
import com.example.rental_property_management_app.network.RetrofitClient
import com.example.rental_property_management_app.utils.Resource
import org.json.JSONObject
import retrofit2.Response

class GuestStayRepository {
    private val api = RetrofitClient.apiService

    suspend fun getTenantGuestStays(status: String? = null): Resource<List<GuestStayResponse>> =
        safeCall<List<GuestStayResponse>> {
            val res = api.getTenantGuestStays(status)
            if (res.isSuccessful && res.body()?.success == true) {
                Resource.Success(res.body()?.data?.content.orEmpty())
            } else {
                Resource.Error(parseError(res, "Không thể tải danh sách khách lưu trú"))
            }
        }

    suspend fun createGuestStay(request: CreateGuestStayRequest): Resource<GuestStayResponse> =
        safeCall<GuestStayResponse> {
            val res = api.createTenantGuestStay(request)
            val data = res.body()?.data
            if (res.isSuccessful && res.body()?.success == true && data != null) {
                Resource.Success(data)
            } else {
                Resource.Error(parseError(res, "Không thể gửi khai báo khách lưu trú"))
            }
        }

    suspend fun getTenantGuestStayDetail(id: Long): Resource<GuestStayResponse> =
        safeCall<GuestStayResponse> {
            val res = api.getTenantGuestStayDetail(id)
            val data = res.body()?.data
            if (res.isSuccessful && res.body()?.success == true && data != null) {
                Resource.Success(data)
            } else {
                Resource.Error(parseError(res, "Không tìm thấy khai báo khách lưu trú"))
            }
        }

    suspend fun cancelGuestStay(id: Long): Resource<GuestStayResponse> =
        safeCall<GuestStayResponse> {
            val res = api.cancelTenantGuestStay(id)
            val data = res.body()?.data
            if (res.isSuccessful && res.body()?.success == true && data != null) {
                Resource.Success(data)
            } else {
                Resource.Error(parseError(res, "Không thể hủy khai báo"))
            }
        }

    suspend fun deleteGuestStay(id: Long): Resource<Unit> =
        safeCall<Unit> {
            val res = api.deleteTenantGuestStay(id)
            if (res.isSuccessful && res.body()?.success == true) {
                Resource.Success(Unit)
            } else {
                Resource.Error(parseError(res, "Không thể xóa khai báo"))
            }
        }

    suspend fun getLandlordGuestStays(status: String? = null): Resource<List<GuestStayResponse>> =
        safeCall<List<GuestStayResponse>> {
            val res = api.getLandlordGuestStays(status)
            if (res.isSuccessful && res.body()?.success == true) {
                Resource.Success(res.body()?.data?.content.orEmpty())
            } else {
                Resource.Error(parseError(res, "Không thể tải danh sách khách lưu trú"))
            }
        }

    suspend fun getLandlordGuestStayDetail(id: Long): Resource<GuestStayResponse> =
        safeCall<GuestStayResponse> {
            val res = api.getLandlordGuestStayDetail(id)
            val data = res.body()?.data
            if (res.isSuccessful && res.body()?.success == true && data != null) {
                Resource.Success(data)
            } else {
                Resource.Error(parseError(res, "Không tìm thấy khai báo khách lưu trú"))
            }
        }

    suspend fun updateGuestStayStatus(
        id: Long,
        status: String,
        landlordNote: String?
    ): Resource<GuestStayResponse> =
        safeCall<GuestStayResponse> {
            val res = api.updateLandlordGuestStayStatus(id, UpdateGuestStayStatusRequest(status, landlordNote))
            val data = res.body()?.data
            if (res.isSuccessful && res.body()?.success == true && data != null) {
                Resource.Success(data)
            } else {
                Resource.Error(parseError(res, "Không thể cập nhật trạng thái"))
            }
        }

    private suspend fun <T> safeCall(block: suspend () -> Resource<T>): Resource<T> = try {
        block()
    } catch (_: Exception) {
        Resource.Error("Không thể kết nối máy chủ. Vui lòng kiểm tra Internet và thử lại.")
    }

    private fun parseError(response: Response<*>, fallback: String): String {
        val raw = response.errorBody()?.string()
        if (!raw.isNullOrBlank()) {
            try {
                val msg = JSONObject(raw).optString("message")
                if (msg.isNotBlank()) return msg
            } catch (_: Exception) {
            }
        }
        return fallback
    }
}
