package com.roomcare.backend.controller;

import com.roomcare.backend.dto.ApiResponse;
import com.roomcare.backend.dto.PageResponse;
import com.roomcare.backend.dto.gueststay.GuestStayResponse;
import com.roomcare.backend.dto.gueststay.UpdateGuestStayStatusRequest;
import com.roomcare.backend.entity.GuestStay;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.service.GuestStayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/landlord/guest-stays")
@RequiredArgsConstructor
public class LandlordGuestStayController {

    private final GuestStayService service;

    @GetMapping
    public ApiResponse<PageResponse<GuestStayResponse>> list(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) GuestStay.Status status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(service.listForLandlord(user, status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<GuestStayResponse> get(
            @AuthenticationPrincipal User user,
            @PathVariable Long id
    ) {
        return ApiResponse.success(service.getForLandlord(user, id));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<GuestStayResponse> updateStatus(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody UpdateGuestStayStatusRequest request
    ) {
        return ApiResponse.success("Đã cập nhật trạng thái khai báo khách lưu trú", service.updateStatus(user, id, request));
    }
}
