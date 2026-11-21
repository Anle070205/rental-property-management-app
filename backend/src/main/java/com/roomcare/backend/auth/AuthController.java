package com.roomcare.backend.auth;

import com.roomcare.backend.dto.ActivateAccountRequest;
import com.roomcare.backend.dto.ApiResponse;
import com.roomcare.backend.dto.AuthResponse;
import com.roomcare.backend.dto.ForgotPasswordRequest;
import com.roomcare.backend.dto.LoginRequest;
import com.roomcare.backend.dto.RegisterRequest;
import com.roomcare.backend.dto.ResendOtpRequest;
import com.roomcare.backend.dto.ResetPasswordRequest;
import com.roomcare.backend.dto.VerifyOtpRequest;
import com.roomcare.backend.dto.VerifyResetOtpRequest;
import com.roomcare.backend.dto.VerifyResetOtpResponse;
import com.roomcare.backend.entity.AuthVerificationToken;
import com.roomcare.backend.service.TenantAccountService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.HtmlUtils;

/**
 * Authentication Controller - Đăng ký & xác thực OTP (Email Gmail / SĐT Việt Nam),
 * đăng nhập và khôi phục mật khẩu.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @Autowired(required = false)
    private TenantAccountService tenantAccountService;

    @PostMapping("/register/validate")
    public ResponseEntity<ApiResponse<Void>> validateBeforeRegister(
            @Valid @RequestBody RegisterRequest request
    ) {
        authService.validateBeforeRegister(request);
        return ResponseEntity.ok(ApiResponse.success("Thông tin đăng ký hợp lệ. Vui lòng chọn phương thức xác thực."));
    }

    public record SwitchChannelRequest(
            @jakarta.validation.constraints.NotNull AuthVerificationToken.Channel currentChannel,
            @jakarta.validation.constraints.NotBlank String currentTarget,
            @jakarta.validation.constraints.NotNull AuthVerificationToken.Channel newChannel
    ) {}

    @PostMapping("/register/switch-channel")
    public ResponseEntity<ApiResponse<AuthResponse>> switchRegistrationChannel(
            @Valid @RequestBody SwitchChannelRequest request,
            HttpServletRequest httpRequest
    ) {
        AuthResponse response = authService.switchRegistrationChannel(
                request.currentChannel(),
                request.currentTarget(),
                request.newChannel(),
                resolveClientIp(httpRequest)
        );
        String message = AuthVerificationToken.Channel.PHONE.name().equals(response.getVerificationChannel())
                ? "Đã chuyển sang SMS OTP. Vui lòng kiểm tra tin nhắn trên số điện thoại của bạn."
                : "Đã chuyển sang Gmail. Vui lòng kiểm tra hộp thư để lấy mã OTP.";
        return ResponseEntity.ok(ApiResponse.success(message, response));
    }

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpRequest
    ) {
        AuthResponse response = authService.register(request, resolveClientIp(httpRequest));
        String message = AuthVerificationToken.Channel.PHONE.name().equals(response.getVerificationChannel())
                ? "Đã tạo yêu cầu đăng ký. Vui lòng nhập mã OTP vừa gửi qua SMS tới số điện thoại của bạn."
                : "Đã tạo yêu cầu đăng ký. Vui lòng kiểm tra Gmail để nhập mã OTP xác thực.";
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(message, response));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<ApiResponse<Void>> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request
    ) {
        authService.verifyOtp(request);
        String msg = request.channel() == AuthVerificationToken.Channel.EMAIL
                ? "Xác thực Gmail thành công. Bạn có thể đăng nhập ngay."
                : "Xác thực số điện thoại thành công. Bạn có thể đăng nhập ngay.";
        return ResponseEntity.ok(ApiResponse.success(msg));
    }

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

    @PostMapping("/resend-otp")
    public ResponseEntity<ApiResponse<Void>> resendOtp(
            @Valid @RequestBody ResendOtpRequest request,
            HttpServletRequest httpRequest
    ) {
        authService.resendOtp(request, resolveClientIp(httpRequest));
        String msg = request.channel() == AuthVerificationToken.Channel.EMAIL
                ? "Nếu tài khoản tồn tại và chưa xác thực, mã OTP mới đã được gửi đến Gmail của bạn."
                : "Nếu tài khoản tồn tại và chưa xác thực, mã OTP mới đã được gửi qua SMS đến số điện thoại của bạn.";
        return ResponseEntity.ok(ApiResponse.success(msg));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request
    ) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success("Đăng nhập thành công", response));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok(ApiResponse.success("Đăng xuất thành công"));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<Void>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request,
            HttpServletRequest httpRequest
    ) {
        authService.forgotPassword(request, resolveClientIp(httpRequest));
        String msg = request.channel() == AuthVerificationToken.Channel.EMAIL
                ? "Nếu địa chỉ Gmail tồn tại trong hệ thống, RoomCare đã gửi mã OTP khôi phục mật khẩu đến hộp thư của bạn."
                : "Nếu số điện thoại tồn tại trong hệ thống, RoomCare đã gửi mã OTP khôi phục mật khẩu qua SMS.";
        return ResponseEntity.ok(ApiResponse.success(msg));
    }

    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<ApiResponse<VerifyResetOtpResponse>> verifyResetOtp(
            @Valid @RequestBody VerifyResetOtpRequest request
    ) {
        VerifyResetOtpResponse response = authService.verifyResetOtp(request);
        return ResponseEntity.ok(ApiResponse.success("Xác minh mã OTP thành công", response));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request
    ) {
        authService.resetPassword(request);
        return ResponseEntity.ok(
                ApiResponse.success("Đặt lại mật khẩu thành công. Vui lòng đăng nhập lại với mật khẩu mới.")
        );
    }

    @PostMapping("/activate-account")
    public ResponseEntity<ApiResponse<Void>> activateAccount(
            @Valid @RequestBody ActivateAccountRequest request
    ) {
        if (tenantAccountService == null) {
            throw new IllegalStateException("Dịch vụ kích hoạt tài khoản chưa sẵn sàng");
        }
        tenantAccountService.activateAccount(request);
        return ResponseEntity.ok(
                ApiResponse.success("Kích hoạt tài khoản và thiết lập mật khẩu thành công. Vui lòng đăng nhập.")
        );
    }

    private String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
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
