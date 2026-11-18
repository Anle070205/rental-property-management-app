package com.example.rental_property_management_app.network

import com.example.rental_property_management_app.models.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API Service Interface
 */
interface ApiService {
    
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

    // ============================================
    // PHASE 4 - INVOICE APIs (GĐ4.1)
    // ============================================
    @GET("api/landlord/invoices")
    suspend fun getInvoices(
        @Query("month") month: String? = null,
        @Query("roomId") roomId: Long? = null,
        @Query("propertyId") propertyId: Long? = null,
        @Query("status") status: String? = null
    ): Response<ApiResponse<List<InvoiceResponse>>>

    @POST("api/landlord/invoices")
    suspend fun createInvoice(
        @Body request: CreateInvoiceRequest
    ): Response<ApiResponse<InvoiceResponse>>

    @POST("api/landlord/invoices/batch")
    suspend fun batchCreateInvoices(
        @Body request: BatchCreateInvoiceRequest
    ): Response<ApiResponse<BatchCreateInvoiceResult>>
}
