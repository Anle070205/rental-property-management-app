package com.example.rental_property_management_app.network

import com.example.rental_property_management_app.models.*
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit API Service Interface
 */
interface ApiService {
    @GET("api/tenant/invoices/{id}/payment-info")
    suspend fun tenantPaymentInfo(@Path("id") id: Long): Response<ApiResponse<TenantPaymentInfo>>

    @GET("api/tenant/payments")
    suspend fun tenantPayments(@Query("page") page: Int): Response<ApiResponse<PageResponse<TenantPayment>>>

    @GET("api/tenant/invoices")
    suspend fun tenantInvoices(
        @Query("month") month: String?, @Query("status") status: TenantInvoiceStatus?, @Query("page") page: Int
    ): Response<ApiResponse<PageResponse<TenantInvoice>>>

    @GET("api/tenant/invoices/{id}")
    suspend fun tenantInvoice(@Path("id") id: Long): Response<ApiResponse<TenantInvoice>>

    @POST("api/tenant/invoices/{id}/payment-proof")
    suspend fun submitTenantPaymentProof(
        @Path("id") id: Long,
        @Body request: SubmitPaymentProofRequest
    ): Response<ApiResponse<TenantInvoice>>

    @GET("api/tenant/my-contract")
    suspend fun myContract(): Response<ApiResponse<MyContract>>

    @POST("api/tenant/contracts/{id}/renewal-requests")
    suspend fun requestContractRenewal(@Path("id") id: Long): Response<ApiResponse<Unit>>

    @GET("api/tenant/my-room")
    suspend fun myRoom(): Response<ApiResponse<MyRoom>>

    @GET("api/tenant/dashboard")
    suspend fun tenantDashboard(): Response<ApiResponse<TenantDashboard>>

    // ============================================
    // TENANT MARKETPLACE & BOOKING APIs
    // ============================================
    @GET("api/tenant/marketplace/properties")
    suspend fun browseProperties(
        @Query("keyword") keyword: String? = null,
        @Query("onlyVacant") onlyVacant: Boolean? = null
    ): Response<ApiResponse<List<MarketplaceProperty>>>

    @GET("api/tenant/marketplace/properties/{propertyId}/rooms")
    suspend fun browsePropertyRooms(
        @Path("propertyId") propertyId: Long,
        @Query("status") status: String? = null,
        @Query("keyword") keyword: String? = null
    ): Response<ApiResponse<List<MarketplaceRoom>>>

    @GET("api/tenant/marketplace/rooms/{roomId}")
    suspend fun getMarketplaceRoomDetail(
        @Path("roomId") roomId: Long
    ): Response<ApiResponse<MarketplaceRoom>>

    @POST("api/tenant/bookings")
    suspend fun createBookingOrContact(
        @Body request: CreateBookingRequest
    ): Response<ApiResponse<BookingRequestItem>>

    @GET("api/tenant/bookings")
    suspend fun getTenantBookings(): Response<ApiResponse<List<BookingRequestItem>>>

    @PUT("api/tenant/bookings/{id}/cancel")
    suspend fun cancelTenantBooking(
        @Path("id") id: Long
    ): Response<ApiResponse<BookingRequestItem>>

    // ============================================
    // LANDLORD BOOKING & CONTACT MANAGEMENT APIs
    // ============================================
    @GET("api/landlord/bookings")
    suspend fun getLandlordBookings(
        @Query("status") status: String? = null,
        @Query("requestType") requestType: String? = null
    ): Response<ApiResponse<List<BookingRequestItem>>>

    @PUT("api/landlord/bookings/{id}/respond")
    suspend fun respondLandlordBooking(
        @Path("id") id: Long,
        @Body request: RespondBookingRequest
    ): Response<ApiResponse<BookingRequestItem>>

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

    @POST("api/users/me/contact/request-otp")
    suspend fun requestContactOtp(@Body request: RequestContactOtpRequest): Response<ApiResponse<Unit>>

    @POST("api/users/me/contact/verify-otp")
    suspend fun verifyContactOtp(@Body request: VerifyContactOtpRequest): Response<ApiResponse<ProfileResponse>>
    
    // ============================================
    // TEST API
    // ============================================
    @GET("api/test")
    suspend fun testApi(): Response<Map<String, Any>>
    
    // ============================================
    // AUTHENTICATION APIs
    // ============================================
    @POST("api/auth/register/validate")
    suspend fun validateBeforeRegister(
        @Body request: RegisterRequest
    ): Response<ApiResponse<Unit>>

    @POST("api/auth/register/switch-channel")
    suspend fun switchRegistrationChannel(
        @Body request: SwitchRegistrationChannelRequest
    ): Response<ApiResponse<AuthResponse>>

    @POST("api/auth/register")
    suspend fun register(
        @Body request: RegisterRequest
    ): Response<ApiResponse<AuthResponse>>

    @POST("api/auth/verify-otp")
    suspend fun verifyOtp(
        @Body request: VerifyOtpRequest
    ): Response<ApiResponse<Unit>>

    @POST("api/auth/resend-otp")
    suspend fun resendOtp(
        @Body request: ResendOtpRequest
    ): Response<ApiResponse<Unit>>
    
    @POST("api/auth/login")
    suspend fun login(
        @Body request: LoginRequest
    ): Response<ApiResponse<AuthResponse>>
    
    @POST("api/auth/logout")
    suspend fun logout(): Response<ApiResponse<Unit>>
    
    @POST("api/auth/forgot-password")
    suspend fun forgotPassword(
        @Body request: ForgotPasswordRequest
    ): Response<ApiResponse<Unit>>

    @POST("api/auth/forgot-password/verify-otp")
    suspend fun verifyResetOtp(
        @Body request: VerifyResetOtpRequest
    ): Response<ApiResponse<VerifyResetOtpResponse>>

    @POST("api/auth/reset-password")
    suspend fun resetPassword(
        @Body request: ResetPasswordRequest
    ): Response<ApiResponse<Unit>>

    @POST("api/auth/activate-account")
    suspend fun activateAccount(
        @Body request: ActivateAccountRequest
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

    @POST("api/landlord/tenants/{id}/provision-account")
    suspend fun provisionTenantAccount(
        @Path("id") id: Long,
        @Body request: ProvisionTenantAccountRequest
    ): Response<ApiResponse<ProvisionTenantAccountResponse>>

    @POST("api/landlord/tenants/{id}/resend-activation")
    suspend fun resendTenantActivation(
        @Path("id") id: Long,
        @Query("channel") channel: String
    ): Response<ApiResponse<ProvisionTenantAccountResponse>>

    // ============================================
    // CONTRACT & ROOMMATE APIs (Landlord)
    // ============================================
    @GET("api/landlord/contracts")
    suspend fun getContracts(
        @Query("propertyId") propertyId: Long? = null,
        @Query("roomId") roomId: Long? = null,
        @Query("tenantId") tenantId: Long? = null,
        @Query("status") status: String? = null
    ): Response<ApiResponse<List<ContractResponse>>>

    @GET("api/landlord/contracts/{id}")
    suspend fun getContractById(
        @Path("id") id: Long
    ): Response<ApiResponse<ContractResponse>>

    @POST("api/landlord/contracts")
    suspend fun createContract(
        @Body request: CreateContractRequest
    ): Response<ApiResponse<ContractResponse>>

    @PUT("api/landlord/contracts/{id}")
    suspend fun updateContract(
        @Path("id") id: Long,
        @Body request: UpdateContractRequest
    ): Response<ApiResponse<ContractResponse>>

    @POST("api/landlord/contracts/{id}/sign")
    suspend fun signContract(
        @Path("id") id: Long
    ): Response<ApiResponse<ContractResponse>>

    @POST("api/landlord/contracts/{id}/activate")
    suspend fun activateContract(
        @Path("id") id: Long
    ): Response<ApiResponse<ContractResponse>>

    @POST("api/landlord/contracts/{id}/terminate")
    suspend fun terminateContract(
        @Path("id") id: Long,
        @Body request: TerminateContractRequest = TerminateContractRequest()
    ): Response<ApiResponse<ContractResponse>>

    @GET("api/landlord/roommates")
    suspend fun getRoommates(
        @Query("tenantId") tenantId: Long? = null,
        @Query("roomId") roomId: Long? = null,
        @Query("contractId") contractId: Long? = null
    ): Response<ApiResponse<List<RoommateResponse>>>

    @POST("api/landlord/roommates")
    suspend fun addRoommate(
        @Body request: CreateRoommateRequest
    ): Response<ApiResponse<RoommateResponse>>

    @PUT("api/landlord/roommates/{id}")
    suspend fun updateRoommate(
        @Path("id") id: Long,
        @Body request: UpdateRoommateRequest
    ): Response<ApiResponse<RoommateResponse>>

    @DELETE("api/landlord/roommates/{id}")
    suspend fun deleteRoommate(
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

    @POST("api/landlord/invoices/preview")
    suspend fun previewInvoice(
        @Body request: CreateInvoiceRequest
    ): Response<ApiResponse<InvoiceResponse>>

    @POST("api/landlord/invoices")
    suspend fun createInvoice(
        @Body request: CreateInvoiceRequest
    ): Response<ApiResponse<InvoiceResponse>>

    @PUT("api/landlord/invoices/{id}")
    suspend fun updateDraftInvoice(
        @Path("id") id: Long,
        @Body request: CreateInvoiceRequest
    ): Response<ApiResponse<InvoiceResponse>>

    @POST("api/landlord/invoices/{id}/publish")
    suspend fun publishInvoice(
        @Path("id") id: Long
    ): Response<ApiResponse<InvoiceResponse>>

    @POST("api/landlord/invoices/{id}/adjust")
    suspend fun adjustInvoice(
        @Path("id") id: Long,
        @Body request: AdjustInvoiceRequest
    ): Response<ApiResponse<InvoiceResponse>>

    @POST("api/landlord/invoices/{id}/cancel")
    suspend fun cancelInvoice(
        @Path("id") id: Long,
        @Body request: CancelInvoiceRequest = CancelInvoiceRequest()
    ): Response<ApiResponse<InvoiceResponse>>

    @POST("api/landlord/invoices/batch")
    suspend fun batchCreateInvoices(
        @Body request: BatchCreateInvoiceRequest
    ): Response<ApiResponse<BatchCreateInvoiceResult>>

    @GET("api/landlord/invoices/{id}")
    suspend fun getInvoiceById(
        @Path("id") id: Long
    ): Response<ApiResponse<InvoiceResponse>>

    @POST("api/landlord/invoices/{id}/payments")
    suspend fun recordPayment(
        @Path("id") id: Long,
        @Body request: CreatePaymentRequest
    ): Response<ApiResponse<InvoiceResponse>>

    // ============================================
    // PHASE 4 - UTILITY APIs (GĐ4.2)
    // ============================================
    @GET("api/landlord/utilities")
    suspend fun getUtilities(
        @Query("month") month: String? = null,
        @Query("roomId") roomId: Long? = null,
        @Query("propertyId") propertyId: Long? = null
    ): Response<ApiResponse<List<UtilityRecordResponse>>>

    @GET("api/landlord/utilities/previous-index")
    suspend fun getPreviousUtilityIndex(
        @Query("roomId") roomId: Long,
        @Query("month") month: String? = null
    ): Response<ApiResponse<PreviousUtilityIndexResponse>>

    @POST("api/landlord/utilities")
    suspend fun createUtility(
        @Body request: CreateUtilityRequest
    ): Response<ApiResponse<UtilityRecordResponse>>

    @PUT("api/landlord/utilities/{id}")
    suspend fun updateUtility(
        @Path("id") id: Long,
        @Body request: UpdateUtilityRequest
    ): Response<ApiResponse<UtilityRecordResponse>>

    // ============================================
    // PHASE 4 - DEBT APIs (GĐ4.4)
    // ============================================
    @GET("api/landlord/debts")
    suspend fun getDebts(
        @Query("roomId") roomId: Long? = null,
        @Query("tenantId") tenantId: Long? = null,
        @Query("month") month: String? = null,
        @Query("overdueLevel") overdueLevel: String? = null,
        @Query("keyword") keyword: String? = null
    ): Response<ApiResponse<DebtSummaryResponse>>

    @GET("api/landlord/debts/{tenantId}")
    suspend fun getTenantDebtDetail(
        @Path("tenantId") tenantId: Long
    ): Response<ApiResponse<RoomTenantDebtResponse>>

    // ============================================
    // PHASE 4 - HEALTH SCORE APIs (GĐ4.5)
    // ============================================
    @GET("api/landlord/health-scores")
    suspend fun getHealthScores(
        @Query("ratingLevel") ratingLevel: String? = null,
        @Query("propertyId") propertyId: Long? = null
    ): Response<ApiResponse<HealthScoreOverviewResponse>>

    @GET("api/landlord/rooms/{id}/health-score")
    suspend fun getRoomHealthScore(
        @Path("id") roomId: Long
    ): Response<ApiResponse<RoomHealthScoreResponse>>

    // ============================================
    // PHASE 4 - DASHBOARD API (GĐ4.6)
    // ============================================
    @GET("api/landlord/dashboard")
    suspend fun getLandlordDashboard(): Response<ApiResponse<LandlordDashboardResponse>>

    // ============================================
    // ADMIN APIs
    // ============================================
    @GET("api/admin/dashboard")
    suspend fun getAdminDashboard(): Response<ApiResponse<AdminDashboardResponse>>

    @GET("api/admin/accounts")
    suspend fun getAdminAccounts(
        @Query("role") role: String? = null,
        @Query("isActive") isActive: Boolean? = null,
        @Query("keyword") keyword: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 10
    ): Response<ApiResponse<AdminAccountPageResult>>

    @GET("api/admin/accounts/{id}")
    suspend fun getAdminAccountDetail(@Path("id") id: Long): Response<ApiResponse<AdminAccountItem>>

    @POST("api/admin/accounts")
    suspend fun createAdminAccount(@Body request: AdminCreateAccountRequest): Response<ApiResponse<AdminAccountItem>>

    @PUT("api/admin/accounts/{id}")
    suspend fun updateAdminAccount(
        @Path("id") id: Long,
        @Body request: AdminUpdateAccountRequest
    ): Response<ApiResponse<AdminAccountItem>>

    @POST("api/admin/accounts/{id}/lock")
    suspend fun lockAdminAccount(@Path("id") id: Long): Response<ApiResponse<AdminAccountItem>>

    @POST("api/admin/accounts/{id}/unlock")
    suspend fun unlockAdminAccount(@Path("id") id: Long): Response<ApiResponse<AdminAccountItem>>

    @GET("api/admin/landlords")
    suspend fun getAdminLandlords(@Query("keyword") keyword: String? = null): Response<ApiResponse<AdminLandlordListResult>>

    @GET("api/admin/landlords/{id}")
    suspend fun getAdminLandlordDetail(@Path("id") id: Long): Response<ApiResponse<AdminLandlordItem>>

    @GET("api/admin/properties")
    suspend fun getAdminProperties(@Query("keyword") keyword: String? = null): Response<ApiResponse<AdminPropertyListResult>>

    @GET("api/admin/properties/{id}")
    suspend fun getAdminPropertyDetail(@Path("id") id: Long): Response<ApiResponse<AdminPropertyItem>>

    @GET("api/admin/reports")
    suspend fun getAdminReports(
        @Query("keyword") keyword: String? = null,
        @Query("status") status: String? = null
    ): Response<ApiResponse<AdminReportListResult>>

    @GET("api/admin/reports/{id}")
    suspend fun getAdminReportDetail(@Path("id") id: Long): Response<ApiResponse<AdminReportItem>>

    @POST("api/admin/reports/{id}/resolve")
    suspend fun resolveAdminReport(
        @Path("id") id: Long,
        @Body request: AdminResolveReportRequest
    ): Response<ApiResponse<AdminReportItem>>

    @GET("api/admin/broadcasts/audience-counts")
    suspend fun getAdminBroadcastAudienceCounts(): Response<ApiResponse<AdminBroadcastAudienceCounts>>

    @GET("api/admin/broadcasts")
    suspend fun getAdminBroadcasts(@Query("keyword") keyword: String? = null): Response<ApiResponse<AdminBroadcastListResult>>

    @GET("api/admin/broadcasts/{id}")
    suspend fun getAdminBroadcastDetail(@Path("id") id: Long): Response<ApiResponse<AdminBroadcastItem>>

    @POST("api/admin/broadcasts")
    suspend fun createAdminBroadcast(
        @Body request: AdminBroadcastRequest,
        @Query("sendNow") sendNow: Boolean = false
    ): Response<ApiResponse<AdminBroadcastItem>>

    @PUT("api/admin/broadcasts/{id}")
    suspend fun updateAdminBroadcast(
        @Path("id") id: Long,
        @Body request: AdminBroadcastRequest
    ): Response<ApiResponse<AdminBroadcastItem>>

    @POST("api/admin/broadcasts/{id}/send")
    suspend fun sendAdminBroadcast(@Path("id") id: Long): Response<ApiResponse<AdminBroadcastItem>>

    @PUT("api/admin/profile/password")
    suspend fun changeAdminPassword(@Body request: ChangePasswordRequest): Response<ApiResponse<AdminChangePasswordResponse>>

    // ============================================
    // GUEST STAY (KHAI BÁO KHÁCH LƯU TRÚ & CCCD)
    // ============================================
    @GET("api/tenant/guest-stays")
    suspend fun getTenantGuestStays(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<ApiResponse<PageResponse<GuestStayResponse>>>

    @POST("api/tenant/guest-stays")
    suspend fun createTenantGuestStay(
        @Body request: CreateGuestStayRequest
    ): Response<ApiResponse<GuestStayResponse>>

    @GET("api/tenant/guest-stays/{id}")
    suspend fun getTenantGuestStayDetail(
        @Path("id") id: Long
    ): Response<ApiResponse<GuestStayResponse>>

    @PATCH("api/tenant/guest-stays/{id}/cancel")
    suspend fun cancelTenantGuestStay(
        @Path("id") id: Long
    ): Response<ApiResponse<GuestStayResponse>>

    @DELETE("api/tenant/guest-stays/{id}")
    suspend fun deleteTenantGuestStay(
        @Path("id") id: Long
    ): Response<ApiResponse<Unit>>

    @GET("api/landlord/guest-stays")
    suspend fun getLandlordGuestStays(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50
    ): Response<ApiResponse<PageResponse<GuestStayResponse>>>

    @GET("api/landlord/guest-stays/{id}")
    suspend fun getLandlordGuestStayDetail(
        @Path("id") id: Long
    ): Response<ApiResponse<GuestStayResponse>>

    @PATCH("api/landlord/guest-stays/{id}/status")
    suspend fun updateLandlordGuestStayStatus(
        @Path("id") id: Long,
        @Body request: UpdateGuestStayStatusRequest
    ): Response<ApiResponse<GuestStayResponse>>
}
