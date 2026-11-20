package com.roomcare.backend.controller;

import com.roomcare.backend.dto.ApiResponse;
import com.roomcare.backend.dto.booking.BookingRequestResponse;
import com.roomcare.backend.dto.booking.CreateBookingRequest;
import com.roomcare.backend.dto.booking.MarketplacePropertyResponse;
import com.roomcare.backend.dto.booking.MarketplaceRoomResponse;
import com.roomcare.backend.entity.RoomStatus;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.service.MarketplaceBookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tenant")
@RequiredArgsConstructor
public class TenantMarketplaceController {

    private final MarketplaceBookingService marketplaceBookingService;

    @GetMapping("/marketplace/properties")
    public ApiResponse<List<MarketplacePropertyResponse>> browseProperties(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Boolean onlyVacant) {
        return ApiResponse.success(marketplaceBookingService.browseProperties(keyword, onlyVacant));
    }

    @GetMapping("/marketplace/properties/{propertyId}/rooms")
    public ApiResponse<List<MarketplaceRoomResponse>> browsePropertyRooms(
            @AuthenticationPrincipal User tenantUser,
            @PathVariable Long propertyId,
            @RequestParam(required = false) RoomStatus status,
            @RequestParam(required = false) String keyword) {
        return ApiResponse.success(
                marketplaceBookingService.browsePropertyRooms(tenantUser, propertyId, status, keyword));
    }

    @GetMapping("/marketplace/rooms/{roomId}")
    public ApiResponse<MarketplaceRoomResponse> getRoomDetail(
            @AuthenticationPrincipal User tenantUser,
            @PathVariable Long roomId) {
        return ApiResponse.success(marketplaceBookingService.getMarketplaceRoomDetail(tenantUser, roomId));
    }

    @PostMapping("/bookings")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<BookingRequestResponse> createBookingOrContact(
            @AuthenticationPrincipal User tenantUser,
            @Valid @RequestBody CreateBookingRequest request) {
        return ApiResponse.success(marketplaceBookingService.createRequest(tenantUser, request));
    }

    @GetMapping("/bookings")
    public ApiResponse<List<BookingRequestResponse>> getMyBookings(@AuthenticationPrincipal User tenantUser) {
        return ApiResponse.success(marketplaceBookingService.getTenantRequests(tenantUser));
    }

    @PutMapping("/bookings/{id}/cancel")
    public ApiResponse<BookingRequestResponse> cancelBooking(
            @AuthenticationPrincipal User tenantUser,
            @PathVariable Long id) {
        return ApiResponse.success(marketplaceBookingService.cancelTenantRequest(tenantUser, id));
    }
}
