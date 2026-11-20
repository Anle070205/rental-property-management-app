# Hướng dẫn cấu hình Gmail SMTP & Kiểm thử xác thực Gmail cho RoomCare

Tài liệu này hướng dẫn cấu hình gửi **email Gmail thật** (đăng ký xác thực OTP, gửi lại mã, quên mật khẩu và đặt lại mật khẩu) cho RoomCare Backend và kiểm thử trên Android Emulator hoặc thiết bị thật.

> **Lưu ý bảo mật:** Tuyệt đối **không** commit địa chỉ Gmail thật, Gmail App Password, file `.env` hay token bí mật lên GitHub. Toàn bộ thông tin nhạy cảm được đọc qua biến môi trường.

---

## 1. Bật xác minh 2 bước (2-Step Verification) trên Google Account

Gmail SMTP không chấp nhận mật khẩu đăng nhập Gmail thông thường. Bạn cần một **App Password (Mật khẩu ứng dụng)**:

1. Đăng nhập tài khoản Google dùng làm hộp thư gửi email của hệ thống RoomCare.
2. Truy cập [Google Account Security](https://myaccount.google.com/security).
3. Tại mục **Cách bạn đăng nhập vào Google (How you sign in to Google)**, bật **Xác minh 2 bước (2-Step Verification)** và hoàn tất xác nhận số điện thoại / thiết bị.

---

## 2. Tạo Gmail App Password (16 ký tự)

1. Sau khi bật xác minh 2 bước, mở trang [App Passwords](https://myaccount.google.com/apppasswords) (hoặc tìm `App passwords` trong thanh tìm kiếm của Google Account).
2. Nhập tên ứng dụng: **`RoomCare Backend`** → nhấn **Create**.
3. Google hiển thị mật khẩu ứng dụng 16 ký tự (ví dụ `abcd efgh ijkl mnop`).
4. Khi gán vào `MAIL_PASSWORD`, **bỏ toàn bộ khoảng trắng** (ví dụ `abcdefghijklmnop`) và lưu giữ cẩn thận (chỉ hiển thị một lần).

---

## 3. Cấu hình biến môi trường cho Backend

Tham khảo tệp mẫu [backend/.env.example](../backend/.env.example).

### Windows PowerShell (trước khi chạy `mvn spring-boot:run`)
```powershell
$env:MAIL_HOST = "smtp.gmail.com"
$env:MAIL_PORT = "587"
$env:MAIL_USERNAME = "your-roomcare-sender@gmail.com"
$env:MAIL_PASSWORD = "your16charapppassword"
$env:MAIL_FROM = "your-roomcare-sender@gmail.com"
$env:MAIL_FROM_NAME = "RoomCare"
$env:APP_BASE_URL = "http://localhost:8080"

mvn spring-boot:run -f backend/pom.xml
```

### Linux / macOS (Bash / Zsh)
```bash
export MAIL_HOST="smtp.gmail.com"
export MAIL_PORT="587"
export MAIL_USERNAME="your-roomcare-sender@gmail.com"
export MAIL_PASSWORD="your16charapppassword"
export MAIL_FROM="your-roomcare-sender@gmail.com"
export MAIL_FROM_NAME="RoomCare"
export APP_BASE_URL="http://localhost:8080"

mvn spring-boot:run -f backend/pom.xml
```

### IntelliJ IDEA
`Run/Debug Configurations` → chọn `BackendApplication` → mục **Environment variables** → nhập `MAIL_USERNAME=...;MAIL_PASSWORD=...;MAIL_FROM=...`.

Các tham số bảo mật bổ sung (đã có giá trị mặc định trong `application.properties`):
- `AUTH_OTP_EXPIRATION_MINUTES` (mặc định `10` phút)
- `AUTH_OTP_RESEND_COOLDOWN_SECONDS` (mặc định `60` giây giữa 2 lần gửi)
- `AUTH_OTP_MAX_REQUESTS_PER_HOUR` (mặc định `5` lần/giờ/email)
- `AUTH_OTP_MAX_ATTEMPTS` (mặc định `5` lần nhập sai tối đa cho 1 OTP)

---

## 4. Migration Database (nếu DB RoomCare đã có dữ liệu cũ)

Backend sử dụng `spring.jpa.hibernate.ddl-auto=update`, đồng thời đi kèm script SQL chuẩn tại [backend/database/migration_gmail_auth.sql](../backend/database/migration_gmail_auth.sql) để bảo đảm tương thích dữ liệu cũ trên MySQL 8.x:

```powershell
mysql -h 127.0.0.1 -u root RoomCare < backend/database/migration_gmail_auth.sql
```

Script thực hiện an toàn:
- Thêm `users.email_verified BOOLEAN NOT NULL DEFAULT TRUE` (toàn bộ tài khoản cũ vẫn đăng nhập bình thường).
- Thêm `users.email_verified_at DATETIME NULL`.
- Thêm `users.token_version INT NOT NULL DEFAULT 0` (vô hiệu hóa JWT cũ sau khi đổi hoặc đặt lại mật khẩu).
- Tạo bảng `auth_verification_tokens` lưu SHA-256 hash của OTP và reset ticket.

Nếu khởi tạo mới hoàn toàn database, chạy [backend/database/roomcare_phase2.sql](../backend/database/roomcare_phase2.sql).

---

## 5. Danh sách Auth API (Gmail Verification & Password Reset)

| Method & Path | Mô tả |
|---|---|
| `POST /api/auth/register` | Đăng ký bằng `@gmail.com`, tạo user `emailVerified=false`, gửi OTP 6 số + link đến Gmail thật (không cấp JWT). |
| `POST /api/auth/verify-email` | Body `{"email","otp"}` → xác thực OTP 6 số, kích hoạt `emailVerified=true`. |
| `GET /api/auth/verify-email?email=...&otp=...` | Hỗ trợ xác thực trực tiếp khi bấm liên kết trong email Gmail. |
| `POST /api/auth/resend-verification` | Body `{"email"}` → gửi lại OTP mới (cooldown 60s, tối đa 5 lần/giờ). |
| `POST /api/auth/login` | Chặn đăng nhập nếu `!emailVerified` (`403` + `requiresEmailVerification: true`), cấp JWT gắn `tokenVersion`. |
| `POST /api/auth/forgot-password` | Body `{"email"}` → trả thông điệp trung lập, gửi OTP khôi phục nếu tài khoản tồn tại và đang hoạt động. |
| `POST /api/auth/forgot-password/verify-otp` | Body `{"email","otp"}` → xác minh OTP, cấp `resetToken` dùng 1 lần. |
| `POST /api/auth/reset-password` | Body `{"email","resetToken","newPassword","confirmPassword"}` → cập nhật BCrypt hash, tăng `tokenVersion`, vô hiệu hóa OTP/resetToken. |

---

## 6. Hướng dẫn test trên Android Emulator và Điện thoại thật

### Trên Android Emulator
1. App Android đang cấu hình `Constants.BASE_URL = "http://10.0.2.2:8080/"` (`10.0.2.2` trỏ tới `localhost:8080` của máy host).
2. Khởi chạy backend đã set `MAIL_USERNAME` + `MAIL_PASSWORD`.
3. Cài APK debug mới:
   ```powershell
   adb install -r frontend/app/build/outputs/apk/debug/app-debug.apk
   ```
4. Thực hiện đầy đủ kịch bản:
   - Mở **Đăng ký** → nhập họ tên, **Gmail thật đang dùng**, SĐT, mật khẩu → nhận OTP từ hộp thư Gmail → nhập OTP tại màn **Xác thực Gmail** → đăng nhập lại.
   - Tại màn **Đăng nhập**, nhấn **Quên mật khẩu?** → nhập Gmail đã đăng ký → lấy OTP từ hộp thư Gmail → nhập OTP tại màn **Đặt lại mật khẩu** → nhập mật khẩu mới → đăng nhập lại bằng mật khẩu mới (kiểm tra mật khẩu cũ bị từ chối).

### Trên điện thoại Android thật (qua USB Debugging)
Cách nhanh nhất không cần sửa `BASE_URL`:
```powershell
adb reverse tcp:8080 tcp:8080
```
Hoặc trỏ `BASE_URL` sang IP LAN của máy chạy backend (cùng mạng Wi-Fi, mở cổng `8080` trên Windows Firewall).
