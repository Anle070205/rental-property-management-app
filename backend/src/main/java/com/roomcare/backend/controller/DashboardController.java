package com.roomcare.backend.controller;

import com.roomcare.backend.dto.ApiResponse;
import com.roomcare.backend.dto.dashboard.LandlordDashboardResponse;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/landlord/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public ApiResponse<LandlordDashboardResponse> getDashboard(@AuthenticationPrincipal User landlord) {
        LandlordDashboardResponse response = dashboardService.getLandlordDashboard(landlord.getId());
        return ApiResponse.success(response);
    }
}
