package com.roomcare.backend.controller;

import com.roomcare.backend.dto.ApiResponse;
import com.roomcare.backend.dto.admin.*;
import com.roomcare.backend.dto.user.ChangePasswordRequest;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.service.AdminService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    @GetMapping("/dashboard")
    public ApiResponse<AdminDashboardResponse> dashboard(@AuthenticationPrincipal User admin) {
        return ApiResponse.success(adminService.getDashboard(admin));
    }

    @GetMapping("/accounts")
    public ApiResponse<AdminAccountResponse.PageResult> accounts(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return ApiResponse.success(adminService.listAccounts(role, isActive, keyword, page, size));
    }

    @GetMapping("/accounts/{id}")
    public ApiResponse<AdminAccountResponse> accountDetail(@PathVariable Long id) {
        return ApiResponse.success(adminService.getAccountDetail(id));
    }

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AdminAccountResponse> createAccount(@Valid @RequestBody AdminCreateAccountRequest request) {
        return ApiResponse.success("Đã tạo tài khoản thành công", adminService.createAccount(request));
    }

    @PutMapping("/accounts/{id}")
    public ApiResponse<AdminAccountResponse> updateAccount(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id,
            @Valid @RequestBody AdminUpdateAccountRequest request
    ) {
        return ApiResponse.success("Đã cập nhật tài khoản", adminService.updateAccount(admin.getId(), id, request));
    }

    @PostMapping("/accounts/{id}/lock")
    public ApiResponse<AdminAccountResponse> lockAccount(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id
    ) {
        return ApiResponse.success("Đã khóa tài khoản", adminService.lockAccount(admin.getId(), id));
    }

    @PostMapping("/accounts/{id}/unlock")
    public ApiResponse<AdminAccountResponse> unlockAccount(@PathVariable Long id) {
        return ApiResponse.success("Đã mở khóa tài khoản", adminService.unlockAccount(id));
    }

    @GetMapping("/landlords")
    public ApiResponse<AdminLandlordResponse.ListResult> landlords(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(adminService.listLandlords(keyword));
    }

    @GetMapping("/landlords/{id}")
    public ApiResponse<AdminLandlordResponse> landlordDetail(@PathVariable Long id) {
        return ApiResponse.success(adminService.getLandlordDetail(id));
    }

    @GetMapping("/properties")
    public ApiResponse<AdminPropertyResponse.ListResult> properties(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(adminService.listProperties(keyword));
    }

    @GetMapping("/properties/{id}")
    public ApiResponse<AdminPropertyResponse> propertyDetail(@PathVariable Long id) {
        return ApiResponse.success(adminService.getPropertyDetail(id));
    }

    @GetMapping("/reports")
    public ApiResponse<AdminReportResponse.ListResult> reports(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status
    ) {
        return ApiResponse.success(adminService.listReports(keyword, status));
    }

    @GetMapping("/reports/{id}")
    public ApiResponse<AdminReportResponse> reportDetail(@PathVariable Long id) {
        return ApiResponse.success(adminService.getReportDetail(id));
    }

    @PostMapping("/reports/{id}/resolve")
    public ApiResponse<AdminReportResponse> resolveReport(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id,
            @Valid @RequestBody AdminResolveReportRequest request
    ) {
        return ApiResponse.success("Đã cập nhật kết quả xử lý báo cáo", adminService.resolveReport(admin, id, request));
    }

    @GetMapping("/broadcasts/audience-counts")
    public ApiResponse<AdminBroadcastResponse.AudienceCounts> audienceCounts() {
        return ApiResponse.success(adminService.getAudienceCounts());
    }

    @GetMapping("/broadcasts")
    public ApiResponse<AdminBroadcastResponse.ListResult> broadcasts(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(adminService.listBroadcasts(keyword));
    }

    @GetMapping("/broadcasts/{id}")
    public ApiResponse<AdminBroadcastResponse> broadcastDetail(@PathVariable Long id) {
        return ApiResponse.success(adminService.getBroadcastDetail(id));
    }

    @PostMapping("/broadcasts")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AdminBroadcastResponse> createBroadcast(
            @AuthenticationPrincipal User admin,
            @Valid @RequestBody AdminBroadcastRequest request,
            @RequestParam(defaultValue = "false") boolean sendNow
    ) {
        AdminBroadcastResponse res = adminService.createBroadcast(admin, request, sendNow);
        String msg = sendNow ? "Đã phát hành thông báo thành công" : "Đã lưu bản nháp thông báo";
        return ApiResponse.success(msg, res);
    }

    @PutMapping("/broadcasts/{id}")
    public ApiResponse<AdminBroadcastResponse> updateBroadcast(
            @PathVariable Long id,
            @Valid @RequestBody AdminBroadcastRequest request
    ) {
        return ApiResponse.success("Đã cập nhật bản nháp thông báo", adminService.updateBroadcast(id, request));
    }

    @PostMapping("/broadcasts/{id}/send")
    public ApiResponse<AdminBroadcastResponse> sendBroadcast(
            @AuthenticationPrincipal User admin,
            @PathVariable Long id
    ) {
        return ApiResponse.success("Đã gửi thông báo đến người dùng", adminService.sendBroadcast(admin, id));
    }

    @PutMapping("/profile/password")
    public ApiResponse<AdminChangePasswordResponse> changePassword(
            @AuthenticationPrincipal User admin,
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        AdminChangePasswordResponse res = adminService.changeAdminPassword(admin, request);
        return ApiResponse.success(res.message(), res);
    }
}
