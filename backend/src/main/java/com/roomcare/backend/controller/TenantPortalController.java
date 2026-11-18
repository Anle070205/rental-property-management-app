package com.roomcare.backend.controller;

import com.roomcare.backend.dto.ApiResponse;
import com.roomcare.backend.dto.tenant.MyRoomResponse;
import com.roomcare.backend.dto.tenant.TenantDashboardResponse;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.service.TenantPortalService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenant")
@RequiredArgsConstructor
public class TenantPortalController {
    private final TenantPortalService service;

    @GetMapping("/my-room")
    public ApiResponse<MyRoomResponse> room(@AuthenticationPrincipal User user) {
        return ApiResponse.success(service.room(user));
    }

    @GetMapping("/dashboard")
    public ApiResponse<TenantDashboardResponse> dashboard(@AuthenticationPrincipal User user) {
        return ApiResponse.success(service.dashboard(user));
    }
}
