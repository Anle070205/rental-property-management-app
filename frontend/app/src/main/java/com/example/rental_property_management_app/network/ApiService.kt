package com.example.rental_property_management_app.network

import com.example.rental_property_management_app.models.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API Service Interface
 */
interface ApiService {
    @GET("api/tenant/incidents")
    suspend fun incidents(@Query("status") status: IncidentStatus?, @Query("page") page: Int):
        Response<ApiResponse<PageResponse<Incident>>>

    @POST("api/tenant/incidents")
    suspend fun createIncident(@Body request: CreateIncidentRequest): Response<ApiResponse<Incident>>

    @GET("api/tenant/incidents/{id}")
    suspend fun incident(@Path("id") id: Long): Response<ApiResponse<Incident>>

    @PUT("api/tenant/incidents/{id}")
    suspend fun supplementIncident(@Path("id") id: Long, @Body request: UpdateIncidentRequest):
        Response<ApiResponse<Incident>>

    @GET("api/notifications")
    suspend fun notifications(
        @Query("type") type: NotificationType?,
        @Query("unreadOnly") unreadOnly: Boolean,
        @Query("page") page: Int
    ): Response<ApiResponse<PageResponse<NotificationItem>>>

    @PUT("api/notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: Long): Response<ApiResponse<Unit>>

    @PUT("api/notifications/read-all")
    suspend fun markAllNotificationsRead(): Response<ApiResponse<Unit>>

    @GET("api/notifications/unread-count")
    suspend fun unreadNotificationCount(): Response<ApiResponse<Long>>

    @GET("api/users/me")
    suspend fun getProfile(): Response<ApiResponse<ProfileResponse>>

    @PUT("api/users/me")
    suspend fun updateProfile(@Body request: UpdateProfileRequest): Response<ApiResponse<ProfileResponse>>

    @PUT("api/users/me/password")
    suspend fun changePassword(@Body request: ChangePasswordRequest): Response<ApiResponse<Unit>>
    
    // ============================================
    // TEST API
    // ============================================
    @GET("api/test")
    suspend fun testApi(): Response<Map<String, Any>>
    
    // ============================================
    // AUTHENTICATION APIs
    // ============================================
    @POST("api/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<ApiResponse<AuthResponse>>
    
    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ApiResponse<AuthResponse>>
    
    @POST("api/auth/logout")
    suspend fun logout(): Response<ApiResponse<Unit>>
    
    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(
        @Query("email") email: String
    ): Response<ApiResponse<Unit>>
    
    // ============================================
    // PROPERTY APIs (Landlord)
    // ============================================
    @GET("api/landlord/properties")
    suspend fun getProperties(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10,
        @Query("sortBy") sortBy: String = "createdAt",
        @Query("sortDir") sortDir: String = "desc"
    ): Response<ApiResponse<PageResponse<PropertyResponse>>>
    
    @GET("api/landlord/properties/{id}")
    suspend fun getPropertyById(
        @Path("id") id: Long
    ): Response<ApiResponse<PropertyResponse>>
    
    @POST("api/landlord/properties")
    suspend fun createProperty(
        @Body request: CreatePropertyRequest
    ): Response<ApiResponse<PropertyResponse>>
    
    @PUT("api/landlord/properties/{id}")
    suspend fun updateProperty(
        @Path("id") id: Long,
        @Body request: UpdatePropertyRequest
    ): Response<ApiResponse<PropertyResponse>>
    
    // ============================================
    // ROOM APIs (Landlord)
    // ============================================
    @GET("api/landlord/rooms")
    suspend fun getAllRooms(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sortBy") sortBy: String = "roomCode",
        @Query("sortDir") sortDir: String = "asc"
    ): Response<ApiResponse<PageResponse<RoomResponse>>>
    
    @GET("api/landlord/rooms/property/{propertyId}")
    suspend fun getRoomsByProperty(
        @Path("propertyId") propertyId: Long,
        @Query("status") status: String? = null,
        @Query("minPrice") minPrice: String? = null,
        @Query("maxPrice") maxPrice: String? = null,
        @Query("floor") floor: Int? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
        @Query("sortBy") sortBy: String = "roomCode",
        @Query("sortDir") sortDir: String = "asc"
    ): Response<ApiResponse<PageResponse<RoomResponse>>>
    
    @GET("api/landlord/rooms/{id}")
    suspend fun getRoomById(
        @Path("id") id: Long
    ): Response<ApiResponse<RoomResponse>>
    
    @POST("api/landlord/rooms")
    suspend fun createRoom(
        @Body request: CreateRoomRequest
    ): Response<ApiResponse<RoomResponse>>
    
    @PUT("api/landlord/rooms/{id}")
    suspend fun updateRoom(
        @Path("id") id: Long,
        @Body request: UpdateRoomRequest
    ): Response<ApiResponse<RoomResponse>>
    
    @DELETE("api/landlord/rooms/{id}")
    suspend fun deleteRoom(
        @Path("id") id: Long
    ): Response<ApiResponse<Unit>>
    
    // ============================================
    // TENANT APIs (Landlord)
    // ============================================
    @GET("api/landlord/tenants")
    suspend fun getTenants(
        @Query("propertyId") propertyId: Long? = null,
        @Query("keyword") keyword: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10,
        @Query("sortBy") sortBy: String = "fullName",
        @Query("sortDir") sortDir: String = "asc"
    ): Response<ApiResponse<PageResponse<TenantResponse>>>
    
    @GET("api/landlord/tenants/{id}")
    suspend fun getTenantById(
        @Path("id") id: Long
    ): Response<ApiResponse<TenantResponse>>
    
    @POST("api/landlord/tenants")
    suspend fun createTenant(
        @Body request: CreateTenantRequest
    ): Response<ApiResponse<TenantResponse>>
    
    @PUT("api/landlord/tenants/{id}")
    suspend fun updateTenant(
        @Path("id") id: Long,
        @Body request: UpdateTenantRequest
    ): Response<ApiResponse<TenantResponse>>
    
    @DELETE("api/landlord/tenants/{id}")
    suspend fun deleteTenant(
        @Path("id") id: Long
    ): Response<ApiResponse<Unit>>
}
