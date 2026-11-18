package com.roomcare.backend.controller;

import com.roomcare.backend.dto.ApiResponse;
import com.roomcare.backend.dto.utility.CreateUtilityRequest;
import com.roomcare.backend.dto.utility.PreviousUtilityIndexResponse;
import com.roomcare.backend.dto.utility.UpdateUtilityRequest;
import com.roomcare.backend.dto.utility.UtilityRecordResponse;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.service.UtilityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/landlord/utilities")
@RequiredArgsConstructor
public class UtilityController {

    private final UtilityService utilityService;

    @GetMapping
    public ApiResponse<List<UtilityRecordResponse>> getUtilityRecords(
            @AuthenticationPrincipal User landlord,
            @RequestParam(required = false) String month,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Long propertyId) {
        List<UtilityRecordResponse> responses = utilityService.getUtilityRecords(
                landlord.getId(), month, roomId, propertyId
        );
        return ApiResponse.success(responses);
    }

    @GetMapping("/previous-index")
    public ApiResponse<PreviousUtilityIndexResponse> getPreviousIndex(
            @AuthenticationPrincipal User landlord,
            @RequestParam Long roomId,
            @RequestParam(required = false) String month) {
        PreviousUtilityIndexResponse response = utilityService.getPreviousIndex(
                landlord.getId(), roomId, month
        );
        return ApiResponse.success(response);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UtilityRecordResponse> createUtilityRecord(
            @AuthenticationPrincipal User landlord,
            @Valid @RequestBody CreateUtilityRequest request) {
        UtilityRecordResponse response = utilityService.createUtilityRecord(landlord.getId(), request);
        return ApiResponse.success("Ghi chỉ số điện nước thành công", response);
    }

    @PutMapping("/{id}")
    public ApiResponse<UtilityRecordResponse> updateUtilityRecord(
            @AuthenticationPrincipal User landlord,
            @PathVariable Long id,
            @Valid @RequestBody UpdateUtilityRequest request) {
        UtilityRecordResponse response = utilityService.updateUtilityRecord(landlord.getId(), id, request);
        return ApiResponse.success("Cập nhật chỉ số điện nước thành công", response);
    }
}
