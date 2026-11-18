package com.roomcare.backend.controller;

import com.roomcare.backend.dto.ApiResponse;
import com.roomcare.backend.dto.tenant.MyContractResponse;
import com.roomcare.backend.dto.tenant.MyRoomResponse;
import com.roomcare.backend.dto.tenant.TenantDashboardResponse;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.service.TenantPortalService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenant")
@RequiredArgsConstructor
public class TenantPortalController {
    private final TenantPortalService service;

    @GetMapping("/my-contract")
    public ApiResponse<MyContractResponse> contract(@AuthenticationPrincipal User user) {
        return ApiResponse.success(service.contract(user));
    }

    @PostMapping("/contracts/{id}/renewal-requests")
    public ResponseEntity<ApiResponse<Void>> requestRenewal(@AuthenticationPrincipal User user,
                                                            @PathVariable Long id) {
        if (!service.requestRenewal(user, id)) {
            return ResponseEntity.status(404).body(ApiResponse.error("Chưa có hợp đồng có thể gia hạn"));
        }
        return ResponseEntity.ok(ApiResponse.success("Đã gửi yêu cầu gia hạn"));
    }

    @GetMapping("/my-room")
    public ApiResponse<MyRoomResponse> room(@AuthenticationPrincipal User user) {
        return ApiResponse.success(service.room(user));
    }

    @GetMapping("/dashboard")
    public ApiResponse<TenantDashboardResponse> dashboard(@AuthenticationPrincipal User user) {
        return ApiResponse.success(service.dashboard(user));
    }
}
