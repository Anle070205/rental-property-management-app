package com.roomcare.backend.controller;

import com.roomcare.backend.dto.ApiResponse;
import com.roomcare.backend.dto.debt.DebtSummaryResponse;
import com.roomcare.backend.dto.debt.RoomTenantDebtResponse;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.service.DebtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/landlord/debts")
@RequiredArgsConstructor
public class DebtController {

    private final DebtService debtService;

    @GetMapping
    public ApiResponse<DebtSummaryResponse> getDebts(
            @AuthenticationPrincipal User landlord,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) String overdueLevel,
            @RequestParam(required = false) String keyword) {
        DebtSummaryResponse response = debtService.getDebts(
                landlord.getId(), roomId, tenantId, month, overdueLevel, keyword
        );
        return ApiResponse.success(response);
    }

    @GetMapping("/{tenantId}")
    public ApiResponse<RoomTenantDebtResponse> getTenantDebtDetail(
            @AuthenticationPrincipal User landlord,
            @PathVariable Long tenantId) {
        RoomTenantDebtResponse response = debtService.getTenantDebtDetail(landlord.getId(), tenantId);
        return ApiResponse.success(response);
    }
}
