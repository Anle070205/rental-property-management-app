package com.roomcare.backend.controller;

import com.roomcare.backend.dto.ApiResponse;
import com.roomcare.backend.dto.user.ChangePasswordRequest;
import com.roomcare.backend.dto.user.ProfileResponse;
import com.roomcare.backend.dto.user.RequestContactOtpRequest;
import com.roomcare.backend.dto.user.UpdateProfileRequest;
import com.roomcare.backend.dto.user.VerifyContactOtpRequest;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.service.UserProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserProfileController {
    private final UserProfileService profiles;

    @GetMapping
    public ApiResponse<ProfileResponse> get(@AuthenticationPrincipal User user) {
        return ApiResponse.success(profiles.get(user));
    }

    @PutMapping
    public ApiResponse<ProfileResponse> update(@AuthenticationPrincipal User user,
                                               @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success(profiles.update(user, request));
    }

    @PutMapping("/password")
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal User user,
                                            @Valid @RequestBody ChangePasswordRequest request) {
        profiles.changePassword(user, request);
        return ApiResponse.success("Đã đổi mật khẩu");
    }

    @PostMapping("/contact/request-otp")
    public ApiResponse<Void> requestContactOtp(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody RequestContactOtpRequest request,
            HttpServletRequest httpRequest
    ) {
        String forwarded = httpRequest.getHeader("X-Forwarded-For");
        String ip = (forwarded != null && !forwarded.isBlank())
                ? forwarded.split(",")[0].trim()
                : httpRequest.getRemoteAddr();
        profiles.requestContactOtp(user, request, ip);
        return ApiResponse.success("Đã gửi mã OTP xác thực thông tin liên hệ.");
    }

    @PostMapping("/contact/verify-otp")
    public ApiResponse<ProfileResponse> verifyContactOtp(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody VerifyContactOtpRequest request
    ) {
        return ApiResponse.success("Đã cập nhật và xác thực thông tin liên hệ.", profiles.verifyContactOtp(user, request));
    }
}
