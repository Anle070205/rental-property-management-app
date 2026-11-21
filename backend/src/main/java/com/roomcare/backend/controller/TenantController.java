package com.roomcare.backend.controller;

import com.roomcare.backend.dto.ApiResponse;
import com.roomcare.backend.dto.PageResponse;
import com.roomcare.backend.dto.tenant.CreateTenantRequest;
import com.roomcare.backend.dto.tenant.ProvisionTenantAccountRequest;
import com.roomcare.backend.dto.tenant.ProvisionTenantAccountResponse;
import com.roomcare.backend.dto.tenant.TenantResponse;
import com.roomcare.backend.dto.tenant.UpdateTenantRequest;
import com.roomcare.backend.entity.AuthVerificationToken;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.service.TenantAccountService;
import com.roomcare.backend.service.TenantService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/landlord/tenants")
@RequiredArgsConstructor
public class TenantController {
    
    private final TenantService tenantService;

    @Autowired(required = false)
    private TenantAccountService tenantAccountService;
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TenantResponse> createTenant(
            @AuthenticationPrincipal User landlord,
            @Valid @RequestBody CreateTenantRequest request) {
        TenantResponse response = tenantService.createTenant(landlord.getId(), request);
        return ApiResponse.success(response);
    }
    
    @GetMapping
    public ApiResponse<PageResponse<TenantResponse>> getTenants(
            @AuthenticationPrincipal User landlord,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        PageResponse<TenantResponse> response = tenantService.getTenantsByLandlord(
            landlord.getId(), propertyId, keyword, page, size, sortBy, sortDir
        );
        return ApiResponse.success(response);
    }
    
    @GetMapping("/{id}")
    public ApiResponse<TenantResponse> getTenantById(
            @AuthenticationPrincipal User landlord,
            @PathVariable Long id) {
        TenantResponse response = tenantService.getTenantById(landlord.getId(), id);
        return ApiResponse.success(response);
    }
    
    @PutMapping("/{id}")
    public ApiResponse<TenantResponse> updateTenant(
            @AuthenticationPrincipal User landlord,
            @PathVariable Long id,
            @Valid @RequestBody UpdateTenantRequest request) {
        TenantResponse response = tenantService.updateTenant(landlord.getId(), id, request);
        return ApiResponse.success(response);
    }
    
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteTenant(
            @AuthenticationPrincipal User landlord,
            @PathVariable Long id) {
        tenantService.deleteTenant(landlord.getId(), id);
        return ApiResponse.success("Đã xóa khách thuê thành công");
    }

    @PostMapping("/{id}/provision-account")
    public ApiResponse<ProvisionTenantAccountResponse> provisionAccount(
            @AuthenticationPrincipal User landlord,
            @PathVariable Long id,
            @Valid @RequestBody ProvisionTenantAccountRequest request,
            HttpServletRequest httpRequest) {
        ProvisionTenantAccountResponse response = tenantAccountService.provisionOrLinkAccount(
                landlord.getId(), id, request, resolveClientIp(httpRequest));
        return ApiResponse.success(response.getMessage(), response);
    }

    @PostMapping("/{id}/resend-activation")
    public ApiResponse<ProvisionTenantAccountResponse> resendActivation(
            @AuthenticationPrincipal User landlord,
            @PathVariable Long id,
            @RequestParam(required = false) AuthVerificationToken.Channel channel,
            HttpServletRequest httpRequest) {
        ProvisionTenantAccountResponse response = tenantAccountService.resendActivationOtp(
                landlord.getId(), id, channel, resolveClientIp(httpRequest));
        return ApiResponse.success(response.getMessage(), response);
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) return null;
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
