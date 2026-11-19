package com.roomcare.backend.controller;

import com.roomcare.backend.dto.ApiResponse;
import com.roomcare.backend.dto.PageResponse;
import com.roomcare.backend.dto.tenant.CreateTenantRequest;
import com.roomcare.backend.dto.tenant.TenantResponse;
import com.roomcare.backend.dto.tenant.UpdateTenantRequest;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/landlord/tenants")
@RequiredArgsConstructor
public class TenantController {
    
    private final TenantService tenantService;
    
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
}
