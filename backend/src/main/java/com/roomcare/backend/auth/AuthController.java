package com.roomcare.backend.auth;

import com.roomcare.backend.dto.ApiResponse;
import com.roomcare.backend.dto.AuthResponse;
import com.roomcare.backend.dto.LoginRequest;
import com.roomcare.backend.dto.RegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Authentication Controller - API endpoints cho authentication
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register - Đăng ký tài khoản mới
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        try {
            AuthResponse response = authService.register(request);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(ApiResponse.success("Đăng ký thành công", response));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * POST /api/auth/login - Đăng nhập
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", response));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * POST /api/auth/logout - Đăng xuất
     * Note: Với JWT stateless, logout xử lý ở client (xóa token)
     */
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công"));
    }

    /**
     * POST /api/auth/forgot-password - Quên mật khẩu
     * TODO: Implement email sending logic
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @RequestParam String email
    ) {
        // TODO: Implement forgot password logic with email service
        return ResponseEntity.ok(
                ApiResponse.success("Yêu cầu đặt lại mật khẩu đã được gửi đến email của bạn")
        );
    }
}
