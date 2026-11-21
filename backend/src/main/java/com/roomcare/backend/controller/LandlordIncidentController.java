package com.roomcare.backend.controller;

import com.roomcare.backend.dto.ApiResponse;
import com.roomcare.backend.dto.PageResponse;
import com.roomcare.backend.dto.incident.IncidentResponse;
import com.roomcare.backend.dto.incident.UpdateIncidentStatusRequest;
import com.roomcare.backend.entity.Incident;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.service.IncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/landlord/incidents")
@RequiredArgsConstructor
public class LandlordIncidentController {

    private final IncidentService service;

    @GetMapping
    public ApiResponse<PageResponse<IncidentResponse>> list(
            @AuthenticationPrincipal User user,
            @RequestParam(required = false) Incident.Status status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ApiResponse.success(service.listForLandlord(user, status, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<IncidentResponse> get(
            @AuthenticationPrincipal User user,
            @PathVariable Long id
    ) {
        return ApiResponse.success(service.getForLandlord(user, id));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<IncidentResponse> updateStatus(
            @AuthenticationPrincipal User user,
            @PathVariable Long id,
            @Valid @RequestBody UpdateIncidentStatusRequest request
    ) {
        return ApiResponse.success("Đã cập nhật trạng thái xử lý sự cố", service.updateStatusForLandlord(user, id, request));
    }
}
