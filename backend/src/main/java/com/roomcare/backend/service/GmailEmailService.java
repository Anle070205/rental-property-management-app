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
 * Dịch vụ gửi email qua Gmail SMTP cho luồng xác thực tài khoản và quên mật khẩu.
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
        String subject = "[RoomCare] Xác thực tài khoản Gmail của bạn";
        String safeName = HtmlUtils.htmlEscape(StringUtils.hasText(fullName) ? fullName : "bạn");
        String safeOtp = HtmlUtils.htmlEscape(otpCode);
        String linkBlockHtml = StringUtils.hasText(verificationUrl)
                ? """
                  <p style="margin:16px 0 8px 0;">Hoặc bạn có thể nhấn vào nút bên dưới để xác thực nhanh:</p>
                  <p style="margin:0 0 18px 0;">
                    <a href="%s" style="display:inline-block;background:#2563EB;color:#FFFFFF;text-decoration:none;padding:12px 20px;border-radius:8px;font-weight:bold;">
                      Xác thực tài khoản RoomCare
                    </a>
                  </p>
                  """.formatted(HtmlUtils.htmlEscape(verificationUrl))
                : "";

        String htmlBody = """
                <div style="font-family:Arial,Helvetica,sans-serif;max-width:560px;margin:0 auto;padding:24px;border:1px solid #E2E8F0;border-radius:12px;color:#0F172A;">
                  <h2 style="margin:0 0 12px 0;color:#1D4ED8;">RoomCare - Xác thực Gmail đăng ký</h2>
                  <p>Xin chào <strong>%s</strong>,</p>
                  <p>Cảm ơn bạn đã đăng ký tài khoản tại <strong>RoomCare</strong>. Vui lòng sử dụng mã OTP dưới đây trong ứng dụng để kích hoạt tài khoản:</p>
                  <div style="margin:20px 0;padding:16px;background:#EFF6FF;border:1px dashed #3B82F6;border-radius:10px;text-align:center;">
                    <div style="font-size:28px;font-weight:bold;letter-spacing:6px;color:#1E3A8A;">%s</div>
                    <div style="margin-top:6px;font-size:13px;color:#475569;">Hiệu lực trong %d phút • Chỉ sử dụng một lần</div>
                  </div>
                  %s
                  <p style="font-size:13px;color:#475569;">Nếu bạn không thực hiện đăng ký RoomCare, vui lòng bỏ qua email này. Tuyệt đối không chia sẻ mã OTP cho bất kỳ ai.</p>
                </div>
                """.formatted(safeName, safeOtp, expirationMinutes, linkBlockHtml);

        String linkText = StringUtils.hasText(verificationUrl)
                ? "\nHoặc mở liên kết xác thực: " + verificationUrl + "\n"
                : "";
        String plainBody = """
                RoomCare - Xác thực Gmail đăng ký

                Xin chào %s,
                Mã OTP xác thực tài khoản RoomCare của bạn là: %s
                Mã có hiệu lực trong %d phút và chỉ sử dụng 1 lần.
                %s
                Không chia sẻ mã này cho bất kỳ ai.
                """.formatted(StringUtils.hasText(fullName) ? fullName : "bạn", otpCode, expirationMinutes, linkText);

        sendHtmlEmail(toEmail, subject, plainBody, htmlBody);
    }

    @Override
    public void sendPasswordResetEmail(
            String toEmail,
            String fullName,
            String otpCode,
            int expirationMinutes
    ) {
        String subject = "[RoomCare] Mã OTP đặt lại mật khẩu";
        String safeName = HtmlUtils.htmlEscape(StringUtils.hasText(fullName) ? fullName : "bạn");
        String safeOtp = HtmlUtils.htmlEscape(otpCode);

        String htmlBody = """
                <div style="font-family:Arial,Helvetica,sans-serif;max-width:560px;margin:0 auto;padding:24px;border:1px solid #E2E8F0;border-radius:12px;color:#0F172A;">
                  <h2 style="margin:0 0 12px 0;color:#DC2626;">RoomCare - Khôi phục mật khẩu</h2>
                  <p>Xin chào <strong>%s</strong>,</p>
                  <p>RoomCare vừa nhận được yêu cầu đặt lại mật khẩu cho tài khoản gắn với địa chỉ Gmail này. Hãy nhập mã OTP bên dưới vào ứng dụng để tiếp tục:</p>
                  <div style="margin:20px 0;padding:16px;background:#FEF2F2;border:1px dashed #EF4444;border-radius:10px;text-align:center;">
                    <div style="font-size:28px;font-weight:bold;letter-spacing:6px;color:#991B1B;">%s</div>
                    <div style="margin-top:6px;font-size:13px;color:#475569;">Hiệu lực trong %d phút • Chỉ sử dụng một lần</div>
                  </div>
                  <p style="font-size:13px;color:#475569;">Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này và không cung cấp mã OTP cho người khác.</p>
                </div>
                """.formatted(safeName, safeOtp, expirationMinutes);

        String plainBody = """
                RoomCare - Khôi phục mật khẩu

                Xin chào %s,
                Mã OTP đặt lại mật khẩu RoomCare của bạn là: %s
                Mã có hiệu lực trong %d phút và chỉ sử dụng 1 lần.

                Nếu bạn không yêu cầu đặt lại mật khẩu, hãy bỏ qua email này.
                """.formatted(StringUtils.hasText(fullName) ? fullName : "bạn", otpCode, expirationMinutes);

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
                    "Không thể gửi email đến địa chỉ Gmail. Vui lòng kiểm tra cấu hình SMTP hoặc thử lại sau.",
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
