package com.roomcare.backend.controller;

import com.roomcare.backend.dto.ApiResponse;
import com.roomcare.backend.dto.health.HealthScoreOverviewResponse;
import com.roomcare.backend.dto.health.RoomHealthScoreResponse;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.service.HealthScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/landlord")
@RequiredArgsConstructor
public class HealthScoreController {

    private final HealthScoreService healthScoreService;

    @GetMapping("/health-scores")
    public ApiResponse<HealthScoreOverviewResponse> getAllRoomHealthScores(
            @AuthenticationPrincipal User landlord,
            @RequestParam(required = false) String ratingLevel,
            @RequestParam(required = false) Long propertyId) {
        HealthScoreOverviewResponse response = healthScoreService.getAllRoomHealthScores(
                landlord.getId(), ratingLevel, propertyId
        );
        return ApiResponse.success(response);
    }

    @GetMapping("/rooms/{id}/health-score")
    public ApiResponse<RoomHealthScoreResponse> getRoomHealthScore(
            @AuthenticationPrincipal User landlord,
            @PathVariable Long id) {
        RoomHealthScoreResponse response = healthScoreService.getRoomHealthScore(landlord.getId(), id);
        return ApiResponse.success(response);
    }
}
