package com.roomcare.backend.controller;

import com.roomcare.backend.dto.ApiResponse;
import com.roomcare.backend.dto.contract.*;
import com.roomcare.backend.entity.ContractStatus;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.service.ContractService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/landlord/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;

    @GetMapping
    public ApiResponse<List<ContractResponse>> getContracts(
            @AuthenticationPrincipal User landlord,
            @RequestParam(required = false) Long propertyId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) Long tenantId,
            @RequestParam(required = false) ContractStatus status) {
        return ApiResponse.success(contractService.getContracts(landlord.getId(), propertyId, roomId, tenantId, status));
    }

    @GetMapping("/{id}")
    public ApiResponse<ContractResponse> getContractById(
            @AuthenticationPrincipal User landlord,
            @PathVariable Long id) {
        return ApiResponse.success(contractService.getContractById(landlord.getId(), id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ContractResponse> createContract(
            @AuthenticationPrincipal User landlord,
            @Valid @RequestBody CreateContractRequest request) {
        return ApiResponse.success("Đã tạo hợp đồng nháp (DRAFT)", contractService.createContract(landlord.getId(), request));
    }

    @PutMapping("/{id}")
    public ApiResponse<ContractResponse> updateDraftContract(
            @AuthenticationPrincipal User landlord,
            @PathVariable Long id,
            @Valid @RequestBody UpdateContractRequest request) {
        return ApiResponse.success("Đã cập nhật hợp đồng nháp", contractService.updateDraftContract(landlord.getId(), id, request));
    }

    @PostMapping("/{id}/sign")
    public ApiResponse<ContractResponse> signContract(
            @AuthenticationPrincipal User landlord,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) SignContractRequest request) {
        return ApiResponse.success("Đã xác nhận ký hợp đồng", contractService.signContract(landlord.getId(), id, request));
    }

    @PostMapping("/{id}/activate")
    public ApiResponse<ContractResponse> activateContract(
            @AuthenticationPrincipal User landlord,
            @PathVariable Long id) {
        return ApiResponse.success("Hợp đồng đã chuyển sang trạng thái hiệu lực", contractService.activateContract(landlord.getId(), id));
    }

    @PostMapping("/{id}/terminate")
    public ApiResponse<ContractResponse> terminateContract(
            @AuthenticationPrincipal User landlord,
            @PathVariable Long id,
            @RequestBody(required = false) TerminateContractRequest request) {
        return ApiResponse.success("Đã kết thúc hợp đồng", contractService.terminateContract(landlord.getId(), id, request));
    }
}
