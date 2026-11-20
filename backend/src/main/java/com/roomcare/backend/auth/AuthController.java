package com.roomcare.backend.auth;

import com.roomcare.backend.dto.ApiResponse;
import com.roomcare.backend.dto.AuthResponse;
import com.roomcare.backend.dto.LoginRequest;
import com.roomcare.backend.dto.RegisterRequest;
import com.roomcare.backend.dto.ResendVerificationRequest;
import com.roomcare.backend.dto.VerifyEmailRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

/**
 * Authentication Controller - API endpoints cho authentication và xác thực Gmail
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * POST /api/auth/register - Đăng ký tài khoản mới và gửi OTP xác thực qua Gmail thật
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "Đăng ký thành công. Vui lòng kiểm tra hộp thư Gmail để xác thực tài khoản.",
                        response
                ));
    }

    /**
     * POST /api/auth/verify-email - Xác thực Gmail bằng OTP 6 số
     */
    @PostMapping("/verify-email")
    public ResponseEntity<ApiResponse<Void>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request
    ) {
        authService.verifyEmail(request);
        return ResponseEntity.ok(ApiResponse.success("Xác thực Gmail thành công. Bạn có thể đăng nhập ngay."));
    }

    /**
     * GET /api/auth/verify-email - Xác thực nhanh khi người dùng bấm liên kết trong Gmail
     */
    @GetMapping(value = "/verify-email", produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<String> verifyEmailByLink(
            @RequestParam String email,
            @RequestParam String otp
    ) {
        try {
            authService.verifyEmailByLink(email, otp);
            return ResponseEntity.ok(renderVerificationHtml(
                    true,
                    "Xác thực Gmail thành công",
                    "Tài khoản RoomCare gắn với " + email + " đã được kích hoạt. Bạn có thể quay lại ứng dụng RoomCare và đăng nhập."
            ));
        } catch (RuntimeException ex) {
            return ResponseEntity.badRequest().body(renderVerificationHtml(
                    false,
                    "Không thể xác thực Gmail",
                    ex.getMessage()
            ));
        }
    }

    /**
     * POST /api/auth/resend-verification - Gửi lại email xác thực Gmail
     */
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Void>> resendVerification(
            @Valid @RequestBody ResendVerificationRequest request
    ) {
        authService.resendVerification(request);
        return ResponseEntity.ok(
                ApiResponse.success("Nếu tài khoản tồn tại và chưa xác thực, mã OTP mới đã được gửi đến Gmail của bạn.")
        );
    }

    /**
     * POST /api/auth/login - Đăng nhập
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", response));
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
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @RequestParam String email
    ) {
        return ResponseEntity.ok(
                ApiResponse.success("Yêu cầu đặt lại mật khẩu đã được gửi đến email của bạn")
        );
    }

    private String renderVerificationHtml(boolean success, String title, String detail) {
        String accent = success ? "#16A34A" : "#DC2626";
        String badge = success ? "ĐÃ KÍCH HOẠT" : "XÁC THỰC THẤT BẠI";
        return """
                <!DOCTYPE html>
                <html lang="vi">
                <head>
                  <meta charset="UTF-8"/>
                  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
                  <title>RoomCare - %s</title>
                </head>
                <body style="font-family:Arial,Helvetica,sans-serif;background:#F8FAFC;padding:32px;color:#0F172A;">
                  <div style="max-width:480px;margin:40px auto;background:#FFFFFF;border:1px solid #E2E8F0;border-radius:14px;padding:28px;">
                    <span style="display:inline-block;padding:4px 10px;border-radius:999px;background:%s;color:#FFFFFF;font-size:12px;font-weight:bold;">%s</span>
                    <h2 style="margin:14px 0 8px 0;">%s</h2>
                    <p style="line-height:1.5;color:#334155;">%s</p>
                  </div>
                </body>
                </html>
                """.formatted(
                HtmlUtils.htmlEscape(title),
                accent,
                badge,
                HtmlUtils.htmlEscape(title),
                HtmlUtils.htmlEscape(detail != null ? detail : "")
        );
    }
}
