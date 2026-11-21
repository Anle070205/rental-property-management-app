package com.roomcare.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.roomcare.backend.exception.SmsDeliveryException;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Nhà cung cấp gửi OTP SMS thật thông qua Twilio Verify (khuyến nghị) hoặc Twilio Programmable SMS.
 * Tuyệt đối không log Auth Token hoặc mã OTP.
 */
@Slf4j
@Service
public class TwilioSmsOtpProvider implements SmsOtpProvider {

    private final String accountSid;
    private final String authToken;
    private final String verifyServiceSid;
    private final String fromNumber;
    private final String messagingServiceSid;
    private final Duration requestTimeout;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    @Autowired
    public TwilioSmsOtpProvider(
            @Value("${roomcare.sms.twilio.account-sid:}") String accountSid,
            @Value("${roomcare.sms.twilio.auth-token:}") String authToken,
            @Value("${roomcare.sms.twilio.verify-service-sid:}") String verifyServiceSid,
            @Value("${roomcare.sms.twilio.from-number:}") String fromNumber,
            @Value("${roomcare.sms.twilio.messaging-service-sid:}") String messagingServiceSid,
            @Value("${roomcare.sms.twilio.timeout-seconds:10}") int timeoutSeconds,
            ObjectMapper objectMapper
    ) {
        this(
                accountSid,
                authToken,
                verifyServiceSid,
                fromNumber,
                messagingServiceSid,
                Duration.ofSeconds(Math.max(3, timeoutSeconds)),
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(Math.max(3, timeoutSeconds)))
                        .build(),
                objectMapper
        );
    }

    TwilioSmsOtpProvider(
            String accountSid,
            String authToken,
            String verifyServiceSid,
            String fromNumber,
            String messagingServiceSid,
            Duration requestTimeout,
            HttpClient httpClient,
            ObjectMapper objectMapper
    ) {
        this.accountSid = trimOrEmpty(accountSid);
        this.authToken = trimOrEmpty(authToken);
        this.verifyServiceSid = trimOrEmpty(verifyServiceSid);
        this.fromNumber = trimOrEmpty(fromNumber);
        this.messagingServiceSid = trimOrEmpty(messagingServiceSid);
        this.requestTimeout = requestTimeout;
        this.httpClient = httpClient;
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean isConfigured() {
        boolean hasCredentials = StringUtils.hasText(accountSid) && StringUtils.hasText(authToken);
        boolean hasVerify = StringUtils.hasText(verifyServiceSid);
        boolean hasSmsSender = StringUtils.hasText(fromNumber) || StringUtils.hasText(messagingServiceSid);
        return hasCredentials && (hasVerify || hasSmsSender);
    }

    @Override
    public boolean isProviderManagedVerification() {
        return StringUtils.hasText(accountSid)
                && StringUtils.hasText(authToken)
                && StringUtils.hasText(verifyServiceSid);
    }

    @Override
    public String providerName() {
        return isProviderManagedVerification() ? "TWILIO_VERIFY" : "TWILIO_SMS";
    }

    @Override
    public String sendOtp(String e164Phone, String otpCodeOrNull, String purposeLabel, int expirationMinutes) {
        if (!isConfigured()) {
            throw new SmsDeliveryException(
                    "Hệ thống chưa cấu hình dịch vụ SMS OTP (TWILIO_ACCOUNT_SID, TWILIO_AUTH_TOKEN và TWILIO_VERIFY_SERVICE_SID hoặc TWILIO_FROM_NUMBER)."
            );
        }
        if (isProviderManagedVerification()) {
            return sendViaTwilioVerify(e164Phone);
        }
        return sendViaProgrammableSms(e164Phone, otpCodeOrNull, purposeLabel, expirationMinutes);
    }

    @Override
    public boolean verifyProviderOtp(String e164Phone, String otpCode) {
        if (!isProviderManagedVerification()) {
            throw new SmsDeliveryException("Nhà cung cấp SMS không ở chế độ xác minh tự động.");
        }
        String url = "https://verify.twilio.com/v2/Services/" + urlEncode(verifyServiceSid) + "/VerificationCheck";
        Map<String, String> form = new LinkedHashMap<>();
        form.put("To", e164Phone);
        form.put("Code", otpCode);

        HttpResponse<String> response = postForm(url, form);
        if (response.statusCode() == 404) {
            return false;
        }
        if (response.statusCode() == 429) {
            throw new SmsDeliveryException("Nhà cung cấp SMS tạm khóa xác minh do thử quá nhiều lần. Vui lòng yêu cầu mã mới.");
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new SmsDeliveryException(buildTwilioErrorMessage(response.statusCode(), response.body()));
        }
        JsonNode root = parseJsonQuietly(response.body());
        String status = root != null ? root.path("status").asText("") : "";
        return "approved".equalsIgnoreCase(status);
    }

    private String sendViaTwilioVerify(String e164Phone) {
        String url = "https://verify.twilio.com/v2/Services/" + urlEncode(verifyServiceSid) + "/Verifications";
        Map<String, String> form = new LinkedHashMap<>();
        form.put("To", e164Phone);
        form.put("Channel", "sms");
        form.put("Locale", "vi");

        HttpResponse<String> response = postForm(url, form);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.error("Twilio Verify gửi thất bại tới {} (HTTP {})", maskPhone(e164Phone), response.statusCode());
            throw new SmsDeliveryException(buildTwilioErrorMessage(response.statusCode(), response.body()));
        }
        JsonNode root = parseJsonQuietly(response.body());
        String sid = root != null ? root.path("sid").asText("") : "";
        log.info("Đã gửi yêu cầu Twilio Verify SMS tới {}", maskPhone(e164Phone));
        return sid;
    }

    private String sendViaProgrammableSms(
            String e164Phone,
            String otpCode,
            String purposeLabel,
            int expirationMinutes
    ) {
        if (!StringUtils.hasText(otpCode)) {
            throw new SmsDeliveryException("Thiếu mã OTP để gửi qua Programmable SMS.");
        }
        String url = "https://api.twilio.com/2010-04-01/Accounts/" + urlEncode(accountSid) + "/Messages.json";
        Map<String, String> form = new LinkedHashMap<>();
        form.put("To", e164Phone);
        if (StringUtils.hasText(messagingServiceSid)) {
            form.put("MessagingServiceSid", messagingServiceSid);
        } else {
            form.put("From", fromNumber);
        }
        form.put(
                "Body",
                "[RoomCare] Ma OTP " + purposeLabel + " cua ban la: " + otpCode
                        + ". Hieu luc " + expirationMinutes + " phut. Khong chia se ma nay."
        );

        HttpResponse<String> response = postForm(url, form);
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            log.error("Twilio SMS gửi thất bại tới {} (HTTP {})", maskPhone(e164Phone), response.statusCode());
            throw new SmsDeliveryException(buildTwilioErrorMessage(response.statusCode(), response.body()));
        }
        JsonNode root = parseJsonQuietly(response.body());
        String sid = root != null ? root.path("sid").asText("") : "";
        log.info("Đã gửi SMS OTP tới {}", maskPhone(e164Phone));
        return sid;
    }

    private HttpResponse<String> postForm(String url, Map<String, String> form) {
        String encodedBody = form.entrySet().stream()
                .map(entry -> urlEncode(entry.getKey()) + "=" + urlEncode(entry.getValue()))
                .collect(Collectors.joining("&"));
        String basicAuth = Base64.getEncoder().encodeToString(
                (accountSid + ":" + authToken).getBytes(StandardCharsets.UTF_8)
        );
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(requestTimeout)
                .header("Authorization", "Basic " + basicAuth)
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(encodedBody, StandardCharsets.UTF_8))
                .build();
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new SmsDeliveryException("Quá thời gian chờ phản hồi từ nhà cung cấp SMS.", ex);
        } catch (IOException ex) {
            throw new SmsDeliveryException(
                    "Không thể kết nối đến dịch vụ gửi SMS OTP. Vui lòng kiểm tra mạng hoặc cấu hình Twilio.",
                    ex
            );
        }
    }

    private String buildTwilioErrorMessage(int statusCode, String body) {
        JsonNode root = parseJsonQuietly(body);
        int code = root != null ? root.path("code").asInt(-1) : -1;
        return switch (code) {
            case 21211, 60200 -> "Số điện thoại không hợp lệ hoặc không thể nhận SMS từ nhà cung cấp.";
            case 21408, 60410 -> "Tài khoản SMS chưa bật quyền gửi tới Việt Nam (+84) trên Twilio Geo Permissions.";
            case 21608 -> "Số điện thoại chưa được xác minh trong chế độ Twilio Trial. Vui lòng nâng cấp tài khoản hoặc thêm số Verified Caller IDs.";
            case 60203 -> "Đã vượt quá giới hạn gửi OTP từ nhà cung cấp SMS. Vui lòng thử lại sau.";
            case 20003 -> "Thông tin xác thực Twilio (SID / Auth Token) không hợp lệ.";
            default -> "Không thể gửi OTP SMS lúc này (mã phản hồi " + statusCode + "). Vui lòng kiểm tra cấu hình Twilio hoặc thử lại sau.";
        };
    }

    private JsonNode parseJsonQuietly(String body) {
        if (!StringUtils.hasText(body)) {
            return null;
        }
        try {
            return objectMapper.readTree(body);
        } catch (Exception ignored) {
            return null;
        }
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value != null ? value : "", StandardCharsets.UTF_8);
    }

    private String trimOrEmpty(String value) {
        return value != null ? value.trim() : "";
    }

    private String maskPhone(String phone) {
        if (!StringUtils.hasText(phone) || phone.length() < 6) {
            return "***";
        }
        return phone.substring(0, 4) + "***" + phone.substring(phone.length() - 3);
    }
}
