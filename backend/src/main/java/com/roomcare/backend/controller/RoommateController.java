package com.roomcare.backend.controller;

import com.roomcare.backend.dto.ApiResponse;
import com.roomcare.backend.dto.roommate.CreateRoommateRequest;
import com.roomcare.backend.dto.roommate.RoommateResponse;
import com.roomcare.backend.dto.roommate.UpdateRoommateRequest;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.service.RoommateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/landlord/roommates")
@RequiredArgsConstructor
public class RoommateController {

    private final RoommateService roommateService;

    @GetMapping
    public ApiResponse<List<RoommateResponse>> getRoommates(
            @AuthenticationPrincipal User landlord,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Long contractId) {
        return ApiResponse.success(roommateService.getRoommates(landlord.getId(), tenantId, roomId, contractId));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RoommateResponse> addRoommate(
            @AuthenticationPrincipal User landlord,
            @Valid @RequestBody CreateRoommateRequest request) {
        return ApiResponse.success("Đã thêm người ở cùng", roommateService.addRoommate(landlord.getId(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<RoommateResponse> updateRoommate(
            @AuthenticationPrincipal User landlord,
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoommateRequest request) {
        return ApiResponse.success("Đã cập nhật thông tin người ở cùng", roommateService.updateRoommate(landlord.getId(), id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRoommate(
            @AuthenticationPrincipal User landlord,
            @PathVariable Long id) {
        roommateService.deleteRoommate(landlord.getId(), id);
        return ApiResponse.success("Đã xóa người ở cùng");
    }
}
