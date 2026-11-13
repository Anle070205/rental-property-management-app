package com.roomcare.backend.controller;

import com.roomcare.backend.dto.ApiResponse;
import com.roomcare.backend.dto.PageResponse;
import com.roomcare.backend.dto.property.CreatePropertyRequest;
import com.roomcare.backend.dto.property.PropertyResponse;
import com.roomcare.backend.dto.property.UpdatePropertyRequest;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.service.PropertyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/landlord/properties")
@RequiredArgsConstructor
public class PropertyController {
    
    private final PropertyService propertyService;
    
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PropertyResponse> createProperty(
            @AuthenticationPrincipal User landlord,
            @Valid @RequestBody CreatePropertyRequest request) {
        PropertyResponse response = propertyService.createProperty(landlord.getId(), request);
        return ApiResponse.success(response);
    }
    
    @GetMapping
    public ApiResponse<PageResponse<PropertyResponse>> getProperties(
            @AuthenticationPrincipal User landlord,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        PageResponse<PropertyResponse> response = propertyService.getPropertiesByLandlord(
            landlord.getId(), page, size, sortBy, sortDir
        );
        return ApiResponse.success(response);
    }
    
    @GetMapping("/{id}")
    public ApiResponse<PropertyResponse> getPropertyById(
            @AuthenticationPrincipal User landlord,
            @PathVariable Long id) {
        PropertyResponse response = propertyService.getPropertyById(landlord.getId(), id);
        return ApiResponse.success(response);
    }
    
    @PutMapping("/{id}")
    public ApiResponse<PropertyResponse> updateProperty(
            @AuthenticationPrincipal User landlord,
            @PathVariable Long id,
            @Valid @RequestBody UpdatePropertyRequest request) {
        PropertyResponse response = propertyService.updateProperty(landlord.getId(), id, request);
        return ApiResponse.success(response);
    }
}
