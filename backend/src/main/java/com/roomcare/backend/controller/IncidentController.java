package com.roomcare.backend.controller;

import com.roomcare.backend.dto.ApiResponse;
import com.roomcare.backend.dto.PageResponse;
import com.roomcare.backend.dto.incident.*;
import com.roomcare.backend.entity.Incident;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.service.IncidentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenant/incidents")
@RequiredArgsConstructor
public class IncidentController {
    private final IncidentService service;

    @GetMapping
    public ApiResponse<PageResponse<IncidentResponse>> list(@AuthenticationPrincipal User user,
            @RequestParam(required = false) Incident.Status status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(service.list(user, status, page, size));
    }

    @PostMapping
    public ApiResponse<IncidentResponse> create(@AuthenticationPrincipal User user,
                                                @Valid @RequestBody CreateIncidentRequest request) {
        return ApiResponse.success(service.create(user, request));
    }

    @GetMapping("/{id}")
    public ApiResponse<IncidentResponse> get(@AuthenticationPrincipal User user, @PathVariable Long id) {
        return ApiResponse.success(service.get(user, id));
    }

    @PutMapping("/{id}")
    public ApiResponse<IncidentResponse> supplement(@AuthenticationPrincipal User user, @PathVariable Long id,
                                                    @Valid @RequestBody UpdateIncidentRequest request) {
        return ApiResponse.success(service.supplement(user, id, request));
    }
}
