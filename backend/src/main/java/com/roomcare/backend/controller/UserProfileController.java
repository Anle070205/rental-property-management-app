package com.roomcare.backend.controller;

import com.roomcare.backend.dto.ApiResponse;
import com.roomcare.backend.dto.user.ChangePasswordRequest;
import com.roomcare.backend.dto.user.ProfileResponse;
import com.roomcare.backend.dto.user.UpdateProfileRequest;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.service.UserProfileService;
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
}
