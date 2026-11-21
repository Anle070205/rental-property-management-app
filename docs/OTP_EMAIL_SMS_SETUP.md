# Hướng Dẫn Cấu Hình & Kiểm Thử OTP Gmail + SMS Việt Nam (+84) — RoomCare

Tài liệu này mô tả cấu trúc bảo mật, biến môi trường, nâng cấp cơ sở dữ liệu MySQL và quy trình kiểm thử end-to-end (E2E) thật cho hệ thống xác thực **chọn 1 trong 2 phương thức (Gmail hoặc Số điện thoại +84)** trên RoomCare.

---

## 1. Tổng quan luồng & Chính sách bảo mật OTP

- **Chọn 1 trong 2 phương thức khi đăng ký:**
  - `EMAIL` (Gmail `@gmail.com`): lưu `phone = NULL`, `email_verified = FALSE`. Backend gửi mã OTP 6 chữ số + link xác thực nhanh (`GET /api/auth/verify-email`) qua Gmail SMTP.
  - `PHONE` (Số điện thoại Việt Nam `0[35789]xxxxxxxx` / `84...` / `+84...`): chuẩn hóa về E.164 `+84xxxxxxxxx`, lưu `email = NULL`, `phone_verified = FALSE`. Backend gửi OTP qua Twilio Verify hoặc Twilio Programmable SMS.
- **Chính sách vòng đời OTP (mặc định, cấu hình qua biến môi trường):**
  - Độ dài mã: **6 chữ số** (`SecureRandom`), chỉ sử dụng **1 lần** (`used_at`, `invalidated`).
  - Thời hạn hiệu lực: **`5` phút** (`AUTH_OTP_EXPIRATION_MINUTES=5`).
  - Giới hạn nhập sai: tối đa **`5` lần** (`AUTH_OTP_MAX_ATTEMPTS=5`); sai lần thứ 5 sẽ vô hiệu hóa token ngay (ghi nhận độc lập qua `OtpAttemptService` `REQUIRES_NEW`).
  - Chống spam gửi lại: cooldown **`60` giây** (`AUTH_OTP_RESEND_COOLDOWN_SECONDS=60`), tối đa **`5` lần/giờ** theo tài khoản + kênh (`AUTH_OTP_MAX_REQUESTS_PER_HOUR=5`) và **`20` lần/giờ** theo IP + kênh (`AUTH_OTP_MAX_REQUESTS_PER_IP_PER_HOUR=20`).
  - Lưu trữ an toàn: **không bao giờ lưu OTP plaintext** — băm `SHA-256` kèm kênh, mục đích (`REGISTRATION`, `PASSWORD_RESET`, `CONTACT_LINK`), đích nhận và `AUTH_TOKEN_PEPPER` (hoặc `TWILIO_VERIFY` quản lý trực tiếp trên Twilio Verify).
  - Bảo vệ phiên đăng nhập & đặt lại mật khẩu:
    - Chưa xác thực phương thức đã đăng ký: `POST /api/auth/login` trả HTTP `403` (`UnverifiedAccountException`).
    - Quên mật khẩu (`POST /api/auth/forgot-password`): phản hồi trung tính `200 OK` kể cả khi Gmail/SĐT không tồn tại (anti-enumeration).
    - Đặt lại mật khẩu thành công (`POST /api/auth/reset-password`) hoặc đổi mật khẩu (`POST /api/users/change-password`): tăng `token_version` trong bảng `users`, tự động vô hiệu hóa toàn bộ JWT cũ.
    - Bổ sung / xác thực kênh còn lại trong hồ sơ: dùng OTP riêng (`POST /api/users/contact/request-otp` và `POST /api/users/contact/verify-otp`).
- **Tương thích dữ liệu cũ:** Các tài khoản hiện hữu được giữ nguyên toàn bộ dữ liệu và mặc định `email_verified = TRUE`, `phone_verified = TRUE`, `token_version = 0`. Đăng nhập hỗ trợ cả Email lẫn SĐT dạng `0...` và `+84...`.

---

## 2. Cấu hình Gmail SMTP (Xác thực & Quên mật khẩu qua Gmail)

1. Dùng tài khoản Gmail cá nhân dùng để gửi mail hệ thống RoomCare.
2. Bật **Xác minh 2 bước (2-Step Verification)** tại [Google Account Security](https://myaccount.google.com/security).
3. Tạo **Mật khẩu ứng dụng (App Password)** gồm **16 ký tự** (chọn ứng dụng *Mail* / *Other*), ghi lại chuỗi 16 ký tự viết liền không dấu cách.
4. Sao chép `backend/.env.example` thành `backend/.env` (tệp `.env` đã nằm trong `backend/.gitignore`, **tuyệt đối không commit lên GitHub**):

```env
MAIL_HOST=smtp.gmail.com
MAIL_PORT=587
MAIL_USERNAME=your-roomcare-sender@gmail.com
MAIL_PASSWORD=your16charapppwd
MAIL_FROM=your-roomcare-sender@gmail.com
MAIL_FROM_NAME=RoomCare
APP_BASE_URL=http://localhost:8080
```

---

## 3. Cấu hình SMS Việt Nam (+84) qua Twilio Verify (khuyến nghị)

1. Đăng ký tài khoản Twilio tại [twilio.com](https://www.twilio.com/).
2. Lấy **`Account SID`** (`TWILIO_ACCOUNT_SID`) và **`Auth Token`** (`TWILIO_AUTH_TOKEN`) trên Twilio Console.
3. Tạo một **Verify Service** trong mục *Verify → Services* và lấy **`Service SID`** (`TWILIO_VERIFY_SERVICE_SID`, dạng `VA...`).
   - *(Tùy chọn)* Nếu muốn dùng Twilio Programmable SMS thay vì Verify API, để trống `TWILIO_VERIFY_SERVICE_SID` và cấu hình `TWILIO_FROM_NUMBER` hoặc `TWILIO_MESSAGING_SERVICE_SID`.
4. **Bắt buộc với số Việt Nam (`+84`):**
   - Vào **Twilio Console → Messaging / Verify → Settings → Geo Permissions** và bật quốc gia **Vietnam (+84)**.
   - Nếu đang dùng **Twilio Trial Account**, chỉ các số điện thoại đã được thêm vào danh sách **Verified Caller IDs** mới nhận được SMS thật; khi chạy production cần nâng cấp tài khoản trả phí để gửi SMS tới mọi thuê bao Viettel/VinaPhone/MobiFone (+84) và lưu ý chi phí SMS quốc tế theo bảng giá Twilio.
5. Thiết lập biến môi trường (trong `backend/.env` hoặc biến môi trường shell trước khi chạy Spring Boot):

```env
TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_AUTH_TOKEN=xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_VERIFY_SERVICE_SID=VAxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
TWILIO_TIMEOUT_SECONDS=10
```

> Lưu ý: Khi chưa cấu hình `MAIL_*` hoặc `TWILIO_*`, backend khởi động bình thường và các API nghiệp vụ cũ hoạt động bình thường; khi gọi endpoint gửi email/SMS thật, backend sẽ báo lỗi có kiểm soát `HTTP 503 Service Unavailable` và rollback transaction đăng ký (không lưu tài khoản mồ côi chưa gửi được OTP).

---

## 4. Nâng cấp cơ sở dữ liệu MySQL

Chạy script migration idempotent (an toàn dữ liệu cũ) trước khi khởi động backend lần đầu sau cập nhật:

```powershell
mysql -h 127.0.0.1 -P 3306 -u root RoomCare < backend/database/migration_otp_email_phone.sql
```

Kiểm tra nhanh schema sau khi chạy:

```sql
DESCRIBE users;
DESCRIBE auth_verification_tokens;
SELECT id, full_name, email, phone, email_verified, phone_verified, token_version FROM users;
```

---

## 5. Danh sách kiểm thử thủ công E2E thật (khi đã điền Gmail + Twilio thật)

1. **Khởi động Backend có nạp biến môi trường:**
   - Nạp biến từ `backend/.env` vào PowerShell rồi chạy `mvn spring-boot:run -f backend/pom.xml`.
2. **Kiểm thử luồng A — Đăng ký bằng Gmail thật:**
   - Mở app Android → **Đăng ký** → chọn tab **Gmail** → nhập Họ tên, Gmail thật của bạn, Mật khẩu, Vai trò → nhấn **Gửi mã xác thực**.
   - Kiểm tra hộp thư Gmail đến (hoặc Spam) nhận email có mã OTP 6 chữ số và nút xác thực nhanh.
   - Thử đăng nhập trước khi xác thực → app chặn (`403`) và hướng dẫn sang màn **Xác thực OTP**.
   - Nhập OTP sai 1 lần → báo còn 4 lần; nhập đúng OTP 6 số (hoặc bấm link trong Gmail) → tài khoản kích hoạt thành công.
   - Đăng nhập bằng Gmail vừa kích hoạt → vào đúng màn hình Chủ nhà / Người thuê.
3. **Kiểm thử luồng B — Đăng ký bằng Số điện thoại Việt Nam thật (`+84`):**
   - Mở **Đăng ký** → chọn tab **Số điện thoại** → nhập `09xxxxxxxx` hoặc `+849xxxxxxxx` → nhấn **Gửi mã OTP**.
   - Nhận tin nhắn SMS OTP thật trên điện thoại → nhập OTP 6 số trên màn hình xác thực.
   - Kiểm tra nút **Gửi lại mã** đếm ngược `60s`; bấm ngay trong `60s` sẽ bị giới hạn `429`.
   - Sau khi xác thực thành công, thử đăng nhập lại bằng cả hai dạng `09xxxxxxxx` và `+849xxxxxxxx`.
4. **Kiểm thử Quên mật khẩu & Thu hồi JWT cũ:**
   - Vào **Quên mật khẩu** → chọn tab **Gmail** hoặc **Số điện thoại** → gửi OTP → nhập OTP để lấy `resetToken` (Bước 1/2) → đặt mật khẩu mới (Bước 2/2).
   - Xác minh mật khẩu cũ không đăng nhập được, mật khẩu mới đăng nhập được, và JWT lấy trước khi đổi mật khẩu bị từ chối ở API cần xác thực (`/api/users/me`).
5. **Kiểm thử Bổ sung & Liên kết liên hệ còn lại trong Hồ sơ:**
   - Đăng nhập tài khoản chỉ có Gmail → vào **Hồ sơ** → **Bổ sung / xác thực Gmail hoặc SĐT** → chọn **Số điện thoại** → xác thực OTP SMS thật → kiểm tra trạng thái `(Đã xác thực)` hiển thị ngay sau khi làm mới hồ sơ.
6. **Kiểm thử nghiệp vụ tạo nhà trọ, phòng (`health_score = 100`), đặt phòng & liên hệ:**
   - Đảm bảo tài khoản đăng ký mới chỉ có Email (không có SĐT) hoặc chỉ có SĐT (không có Email) đều tạo nhà trọ/phòng, duyệt đặt phòng, xem hợp đồng/hóa đơn bình thường không crash.
