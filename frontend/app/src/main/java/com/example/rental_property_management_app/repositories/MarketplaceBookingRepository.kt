package com.example.rental_property_management_app.repositories

import com.example.rental_property_management_app.models.*
import com.example.rental_property_management_app.network.RetrofitClient
import com.example.rental_property_management_app.utils.Resource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MarketplaceBookingRepository {
    private val api = RetrofitClient.apiService

    suspend fun browseProperties(
        keyword: String? = null,
        onlyVacant: Boolean? = null
    ): Resource<List<MarketplaceProperty>> = withContext(Dispatchers.IO) {
        ApiResultHandler.execute { api.browseProperties(keyword, onlyVacant) }
    }

    suspend fun browsePropertyRooms(
        propertyId: Long,
        status: RoomStatus? = null,
        keyword: String? = null
    ): Resource<List<MarketplaceRoom>> = withContext(Dispatchers.IO) {
        ApiResultHandler.execute { api.browsePropertyRooms(propertyId, status?.name, keyword) }
    }

    suspend fun getMarketplaceRoomDetail(roomId: Long): Resource<MarketplaceRoom> =
        withContext(Dispatchers.IO) {
            ApiResultHandler.execute { api.getMarketplaceRoomDetail(roomId) }
        }

    suspend fun createBookingOrContact(request: CreateBookingRequest): Resource<BookingRequestItem> =
        withContext(Dispatchers.IO) {
            ApiResultHandler.execute { api.createBookingOrContact(request) }
        }

    suspend fun getTenantBookings(): Resource<List<BookingRequestItem>> =
        withContext(Dispatchers.IO) {
            ApiResultHandler.execute { api.getTenantBookings() }
        }

    suspend fun cancelTenantBooking(id: Long): Resource<BookingRequestItem> =
        withContext(Dispatchers.IO) {
            ApiResultHandler.execute { api.cancelTenantBooking(id) }
        }

    suspend fun getLandlordBookings(
        status: BookingRequestStatus? = null,
        requestType: BookingRequestType? = null
    ): Resource<List<BookingRequestItem>> = withContext(Dispatchers.IO) {
        ApiResultHandler.execute { api.getLandlordBookings(status?.name, requestType?.name) }
    }

    suspend fun respondLandlordBooking(
        id: Long,
        request: RespondBookingRequest
    ): Resource<BookingRequestItem> = withContext(Dispatchers.IO) {
        ApiResultHandler.execute { api.respondLandlordBooking(id, request) }
    }
}
