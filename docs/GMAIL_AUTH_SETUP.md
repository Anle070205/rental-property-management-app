# Cấu hình Gmail SMTP cho RoomCare

Tài liệu này hướng dẫn gửi OTP qua Gmail bằng luồng xác thực dùng chung của RoomCare. Gmail dùng cho xác thực tài khoản và khôi phục mật khẩu; hệ thống cũng hỗ trợ SMS OTP.

## Tạo Gmail App Password

1. Bật xác minh 2 bước cho tài khoản Gmail dùng làm địa chỉ gửi.
2. Tạo App Password trong phần bảo mật tài khoản Google.
3. Dùng App Password 16 ký tự, bỏ khoảng trắng. Không dùng mật khẩu đăng nhập Gmail.

## Cấu hình backend

Thiết lập các biến môi trường trước khi chạy backend. Không commit thông tin thật hoặc tệp `.env`.

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

Các biến OTP tùy chọn có thể đặt theo mẫu trong [backend/.env.example](../backend/.env.example). Mặc định OTP hết hạn sau 5 phút, thời gian chờ gửi lại là 60 giây, tối đa 5 lần gửi mỗi giờ cho một địa chỉ và 5 lần nhập sai cho một OTP.

## API xác thực hiện tại

Các API dưới đây dùng chung cho kênh `EMAIL` và `PHONE`:

- `POST /api/auth/register`: tạo đăng ký và gửi OTP theo kênh đã chọn.
- `POST /api/auth/verify-otp`: xác thực OTP.
- `POST /api/auth/resend-otp`: gửi lại OTP.
- `POST /api/auth/forgot-password`: yêu cầu OTP khôi phục mật khẩu.
- `POST /api/auth/forgot-password/verify-otp`: xác minh OTP và nhận reset ticket.
- `POST /api/auth/reset-password`: đặt mật khẩu mới bằng reset ticket.

Xem request DTO trong `backend/src/main/java/com/roomcare/backend/dto` để biết các trường JSON chính xác. Với Gmail, chọn kênh `EMAIL` và dùng địa chỉ Gmail hợp lệ.

## Cơ sở dữ liệu

Dùng schema hiện hành tại [backend/database/roomcare_phase2.sql](../backend/database/roomcare_phase2.sql). Không chạy migration Gmail-only cũ: cấu trúc token của nhánh develop đã hỗ trợ cả email và SMS, nên script tạo bảng riêng theo email không tương thích.

## Kiểm tra gửi mail

Đăng ký bằng Gmail thử nghiệm và kiểm tra hộp thư đến cùng thư mục spam. Nếu gửi thất bại, kiểm tra lại địa chỉ gửi, App Password, SMTP port 587 và STARTTLS. Không ghi App Password hoặc OTP vào log.