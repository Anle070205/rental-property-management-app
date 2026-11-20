package com.roomcare.backend.service;

import com.roomcare.backend.exception.MailDeliveryException;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

/**
 * Dịch vụ gửi OTP thật qua Gmail SMTP (tuyệt đối không log mật khẩu, App Password hoặc mã OTP).
 */
@Slf4j
@Service
public class GmailEmailService implements EmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String mailUsername;
    private final String mailPassword;
    private final String fromAddress;
    private final String fromName;

    public GmailEmailService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${spring.mail.username:}") String mailUsername,
            @Value("${spring.mail.password:}") String mailPassword,
            @Value("${roomcare.mail.from:}") String fromAddress,
            @Value("${roomcare.mail.from-name:RoomCare}") String fromName
    ) {
        this.mailSenderProvider = mailSenderProvider;
        this.mailUsername = mailUsername != null ? mailUsername.trim() : "";
        this.mailPassword = mailPassword != null ? mailPassword.trim() : "";
        this.fromAddress = fromAddress != null ? fromAddress.trim() : "";
        this.fromName = StringUtils.hasText(fromName) ? fromName.trim() : "RoomCare";
    }

    @Override
    public boolean isConfigured() {
        return StringUtils.hasText(mailUsername)
                && StringUtils.hasText(mailPassword)
                && mailSenderProvider.getIfAvailable() != null;
    }

    @Override
    public void sendRegistrationVerificationEmail(
            String toEmail,
            String fullName,
            String otpCode,
            int expirationMinutes,
            String verificationUrl
    ) {
        String subject = "[RoomCare] Mã OTP xác thực tài khoản Gmail";
        String extraHtml = StringUtils.hasText(verificationUrl)
                ? """
                  <p style="margin:16px 0 8px 0;">Hoặc nhấn nút bên dưới nếu muốn xác thực nhanh trên trình duyệt:</p>
                  <p style="margin:0 0 18px 0;">
                    <a href="%s" style="display:inline-block;background:#2563EB;color:#FFFFFF;text-decoration:none;padding:12px 20px;border-radius:8px;font-weight:bold;">
                      Xác thực tài khoản RoomCare
                    </a>
                  </p>
                  """.formatted(HtmlUtils.htmlEscape(verificationUrl))
                : "";
        sendOtpTemplate(
                toEmail,
                fullName,
                otpCode,
                expirationMinutes,
                subject,
                "#1D4ED8",
                "#EFF6FF",
                "#3B82F6",
                "#1E3A8A",
                "RoomCare - Xác thực đăng ký tài khoản",
                "Cảm ơn bạn đã đăng ký RoomCare bằng Gmail. Hãy nhập mã OTP gồm 6 chữ số bên dưới để kích hoạt tài khoản:",
                extraHtml,
                verificationUrl
        );
    }

    @Override
    public void sendPasswordResetEmail(
            String toEmail,
            String fullName,
            String otpCode,
            int expirationMinutes
    ) {
        sendOtpTemplate(
                toEmail,
                fullName,
                otpCode,
                expirationMinutes,
                "[RoomCare] Mã OTP đặt lại mật khẩu",
                "#DC2626",
                "#FEF2F2",
                "#EF4444",
                "#991B1B",
                "RoomCare - Khôi phục mật khẩu",
                "RoomCare nhận được yêu cầu đặt lại mật khẩu cho tài khoản gắn với địa chỉ Gmail này. Nhập mã OTP 6 chữ số bên dưới để tạo mật khẩu mới:",
                "",
                null
        );
    }

    @Override
    public void sendContactVerificationEmail(
            String toEmail,
            String fullName,
            String otpCode,
            int expirationMinutes
    ) {
        sendOtpTemplate(
                toEmail,
                fullName,
                otpCode,
                expirationMinutes,
                "[RoomCare] Mã OTP xác thực bổ sung Gmail vào hồ sơ",
                "#0F766E",
                "#F0FDFA",
                "#14B8A6",
                "#115E59",
                "RoomCare - Xác thực Gmail liên kết hồ sơ",
                "Bạn đang thực hiện bổ sung địa chỉ Gmail này vào tài khoản RoomCare. Vui lòng nhập mã OTP 6 chữ số dưới đây để xác nhận:",
                "",
                null
        );
    }

    private void sendOtpTemplate(
            String toEmail,
            String fullName,
            String otpCode,
            int expirationMinutes,
            String subject,
            String headingColor,
            String boxBg,
            String boxBorder,
            String codeColor,
            String headingText,
            String introText,
            String extraHtml,
            String optionalUrl
    ) {
        String resolvedName = StringUtils.hasText(fullName) ? fullName.trim() : "bạn";
        String safeName = HtmlUtils.htmlEscape(resolvedName);
        String safeOtp = HtmlUtils.htmlEscape(otpCode);

        String htmlBody = """
                <div style="font-family:Arial,Helvetica,sans-serif;max-width:560px;margin:0 auto;padding:24px;border:1px solid #E2E8F0;border-radius:12px;color:#0F172A;">
                  <h2 style="margin:0 0 12px 0;color:%s;">%s</h2>
                  <p>Xin chào <strong>%s</strong>,</p>
                  <p>%s</p>
                  <div style="margin:20px 0;padding:16px;background:%s;border:1px dashed %s;border-radius:10px;text-align:center;">
                    <div style="font-size:28px;font-weight:bold;letter-spacing:6px;color:%s;">%s</div>
                    <div style="margin-top:6px;font-size:13px;color:#475569;">Hiệu lực trong %d phút • Chỉ sử dụng 1 lần</div>
                  </div>
                  %s
                  <p style="font-size:13px;color:#475569;">Nếu bạn không thực hiện yêu cầu này, hãy bỏ qua email. Tuyệt đối không chia sẻ mã OTP cho người khác.</p>
                </div>
                """.formatted(
                headingColor,
                HtmlUtils.htmlEscape(headingText),
                safeName,
                HtmlUtils.htmlEscape(introText),
                boxBg,
                boxBorder,
                codeColor,
                safeOtp,
                expirationMinutes,
                extraHtml
        );

        String linkLine = StringUtils.hasText(optionalUrl) ? ("\nLiên kết xác thực nhanh: " + optionalUrl + "\n") : "";
        String plainBody = """
                %s

                Xin chào %s,
                %s
                Mã OTP của bạn: %s
                Thời hạn: %d phút (chỉ dùng 1 lần).
                %s
                Tuyệt đối không chia sẻ mã OTP cho bất kỳ ai.
                """.formatted(headingText, resolvedName, introText, otpCode, expirationMinutes, linkLine);

        sendHtmlEmail(toEmail, subject, plainBody, htmlBody);
    }

    private void sendHtmlEmail(String toEmail, String subject, String plainText, String htmlBody) {
        if (!isConfigured()) {
            throw new MailDeliveryException(
                    "Hệ thống chưa cấu hình Gmail SMTP (MAIL_USERNAME / MAIL_PASSWORD). Vui lòng cấu hình App Password và thử lại."
            );
        }
        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            throw new MailDeliveryException("Dịch vụ gửi email SMTP chưa sẵn sàng.");
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());
            String resolvedFrom = StringUtils.hasText(fromAddress) ? fromAddress : mailUsername;
            helper.setFrom(new InternetAddress(resolvedFrom, fromName, StandardCharsets.UTF_8.name()));
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(plainText, htmlBody);
            mailSender.send(mimeMessage);
            log.info("Đã gửi email SMTP '{}' tới {}", subject, maskEmail(toEmail));
        } catch (MailException | MessagingException | UnsupportedEncodingException ex) {
            log.error("Lỗi gửi email SMTP '{}' tới {}: {}", subject, maskEmail(toEmail), ex.getClass().getSimpleName());
            throw new MailDeliveryException(
                    "Không thể gửi email đến Gmail của bạn. Vui lòng kiểm tra cấu hình SMTP hoặc thử lại sau.",
                    ex
            );
        }
    }

    private String maskEmail(String email) {
        if (!StringUtils.hasText(email) || !email.contains("@")) {
            return "***";
        }
        int at = email.indexOf('@');
        if (at <= 2) {
            return "***" + email.substring(at);
        }
        return email.substring(0, 2) + "***" + email.substring(at);
    }
}
