package com.roomcare.backend.controller;

import com.roomcare.backend.dto.ApiResponse;
import com.roomcare.backend.dto.booking.BookingRequestResponse;
import com.roomcare.backend.dto.booking.RespondBookingRequest;
import com.roomcare.backend.entity.BookingRequest;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.service.MarketplaceBookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/landlord/bookings")
@RequiredArgsConstructor
public class LandlordBookingController {

    private final MarketplaceBookingService marketplaceBookingService;

    @GetMapping
    public ApiResponse<List<BookingRequestResponse>> getBookings(
            @AuthenticationPrincipal User landlord,
            @RequestParam(required = false) BookingRequest.Status status,
            @RequestParam(required = false) BookingRequest.RequestType requestType) {
        return ApiResponse.success(marketplaceBookingService.getLandlordRequests(landlord, status, requestType));
    }

    @PutMapping("/{id}/respond")
    public ApiResponse<BookingRequestResponse> respond(
            @AuthenticationPrincipal User landlord,
            @PathVariable Long id,
            @Valid @RequestBody RespondBookingRequest request) {
        return ApiResponse.success(marketplaceBookingService.respondRequest(landlord, id, request));
    }
}
