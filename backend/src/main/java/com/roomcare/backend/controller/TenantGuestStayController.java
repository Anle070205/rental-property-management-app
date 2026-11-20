package com.roomcare.backend.controller;

import com.roomcare.backend.dto.ApiResponse;
import com.roomcare.backend.dto.PageResponse;
import com.roomcare.backend.dto.gueststay.CreateGuestStayRequest;
import com.roomcare.backend.dto.gueststay.GuestStayResponse;
import com.roomcare.backend.entity.GuestStay;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.service.GuestStayService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenant/guest-stays")
@RequiredArgsConstructor
public class TenantGuestStayController {

    private final GuestStayService service;

    @GetMapping
    public ApiResponse<PageResponse<GuestStayResponse>> list(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) GuestStay.Status status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(service.listForTenant(user, status, page, size));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<GuestStayResponse>> create(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CreateGuestStayRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Đã gửi khai báo khách lưu trú tới chủ nhà trọ", service.create(user, request)));
    }

    @GetMapping("/{id}")
    public ApiResponse<GuestStayResponse> get(
            @AuthenticationPrincipal User user,
            @PathVariable Long id
    ) {
        return ApiResponse.success(service.getForTenant(user, id));
    }

    @PatchMapping("/{id}/cancel")
    public ApiResponse<GuestStayResponse> cancel(
            @AuthenticationPrincipal User user,
            @PathVariable Long id
    ) {
        return ApiResponse.success("Đã hủy khai báo khách lưu trú", service.cancelForTenant(user, id));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal User user,
            @PathVariable Long id
    ) {
        service.deleteForTenant(user, id);
        return ApiResponse.success("Đã xóa khai báo khách lưu trú");
    }
}
