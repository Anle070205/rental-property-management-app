# ROOMCARE – BÁO CÁO KIỂM THỬ & TÍCH HỢP (QA REPORT)

## 1. Thông tin chung & Môi trường kiểm thử

- **Nhánh làm việc:** `fix/roomcare-final-testing-ui` (tạo từ `develop` tại commit `49adb8f`)
- **Repository:** `https://github.com/Anle070205/rental-property-management-app`
- **Tài liệu chuẩn phạm vi:** `Ke_hoach_phat_trien_RoomCare_Android_SpringBoot_MySQL.docx` (Phiên bản 2.0)
- **Môi trường kiểm thử thực tế:**
  - OS: Windows
  - Backend: Spring Boot 3.2.0, Java 21 (`C:\Program Files\Java\jdk-21.0.12.1`), Maven 3.9.11, cổng `http://localhost:8080`
  - Database: MySQL 8.4.3 (`127.0.0.1:3306`, schema `RoomCare`)
  - Android Build: JDK 17, AGP / Gradle (`.\gradlew.bat assembleDebug installDebug testDebugUnitTest`)
  - Thiết bị kiểm thử UI/E2E: Android Emulator `Medium_Phone_API_37.0` (`emulator-5554`, `http://10.0.2.2:8080/`)

---

## 2. Bảng kiểm kê Audit toàn dự án (3 Thành viên)

| Module | Màn hình Android | API Backend | Trạng thái | Vấn đề phát hiện khi Audit | Hành động đã xử lý |
|---|---|---|---|---|---|
| **Auth** | `SplashFragment`, `LoginFragment`, `RegisterFragment`, `ForgotPasswordFragment` | `/api/auth/login`, `/api/auth/register`, `/api/auth/logout`, `/api/auth/forgot-password` | **PARTIAL → PASS** (Forgot Password còn stub email ở Backend) | `RegisterFragment` và `AuthService` chỉ cho đăng ký `LANDLORD`; `AuthRepository` nuốt message lỗi HTTP 4xx; hash BCrypt trong `roomcare_phase2.sql` sai không đăng nhập được `"123456"`. | Mở đăng ký chọn vai trò `LANDLORD`/`TENANT`, tự tạo `Property` mặc định cho Chủ nhà mới, tự liên kết `Tenant` cho Người thuê mới; trả lỗi chi tiết từ Backend; sửa hash SQL & `DataSeeder`. |
| **Landlord Dashboard** | `LandlordHomeFragment`, `LandlordDashboardFragment` | `/api/landlord/dashboard` | **FAIL → PASS** | Sau merge `49adb8f`, `nav_graph.xml` trỏ vào `LandlordDashboardFragment` bản rỗng 2 nút, làm `LandlordHomeFragment` (GĐ4.6) thành màn hình chết; thiếu nút Thông báo & Hồ sơ. | Trỏ `landlordDashboardFragment` trực tiếp tới `LandlordHomeFragment`, bổ sung nút Thông báo, Hồ sơ, xác nhận Đăng xuất và tự refresh `onResume()`. |
| **Property & Room** | `RoomListFragment`, `RoomDetailFragment`, `RoomFormFragment`, `PropertySelectionFragment` | `/api/landlord/properties`, `/api/landlord/rooms` | **PARTIAL → PASS** | Nút Back toolbar & tìm kiếm trên `RoomListFragment` còn `TODO`, chưa có lọc trạng thái; `currentTenant` từ Backend trả `null`; `RoomController.deleteRoom` trả `204` làm Android lỗi. | Hoàn thiện tìm kiếm + `ChipGroup` lọc 4 trạng thái, gắn nút Back & Menu refresh; populate `currentTenant` thực tế; kiểm tra ràng buộc trước khi xóa phòng & trả `ApiResponse<Void>`. |
| **Tenant Management (Landlord)** | `TenantListFragment`, `TenantDetailFragment`, `TenantFormFragment` | `/api/landlord/tenants` | **PARTIAL → PASS** | `TenantListFragment` thiếu nút thêm khách; `TenantDetailFragment` thiếu nút Sửa/Xóa & toolbar Back; `TenantFormFragment` gán cứng `propertyId = 1L`; `currentRoom` bị `null`. | Bổ sung FAB thêm mới, nút Sửa/Xóa có hộp thoại xác nhận, tự load nhà trọ đầu tiên của Chủ nhà thay cho `1L`, populate `currentRoom` & kiểm tra công nợ trước khi xóa. |
| **Hợp đồng (GĐ3)** | *(Chưa tách `ContractFragment`/`ContractFormFragment` riêng)* | `GET /api/tenant/my-contract`, `POST /api/tenant/contracts/{id}/renewal-requests` | **PARTIAL** | Nhóm chưa triển khai bảng `contracts` và bộ màn hình CRUD hợp đồng riêng phía Chủ nhà (`/api/landlord/contracts`). | Đã tích hợp hiển thị hợp đồng hiệu lực (`MyContractResponse`) và luồng gửi yêu cầu gia hạn (`NotificationType.CONTRACT`) tới Chủ nhà; giữ nguyên phạm vi tránh phá vỡ schema hiện tại. |
| **Tài chính, Điện nước, Công nợ, Health Score (GĐ4.1–4.5)** | `InvoiceFragment`, `InvoiceDetailFragment`, `UtilityFragment`, `DebtFragment`, `HealthScoreFragment` | `/api/landlord/invoices`, `/utilities`, `/debts`, `/health-scores`, `/rooms/{id}/health-score` | **PASS** | `InvoiceService` chưa gửi thông báo tới Người thuê khi phát hành hóa đơn hoặc xác nhận thanh toán. | Đã nối `NotificationService` gửi thông báo hóa đơn/thanh toán tới tài khoản `TENANT`; giữ nguyên toàn bộ logic tính `BigDecimal` chính xác. |
| **Tenant Portal (GĐ5.1–5.4)** | `TenantHomeFragment`, `MyRoomFragment`, `MyContractFragment`, `TenantInvoiceFragment`, `TenantInvoiceDetailFragment`, `QRPaymentFragment`, `PaymentHistoryFragment` | `/api/tenant/dashboard`, `/my-room`, `/my-contract`, `/contracts/{id}/renewal-requests`, `/invoices`, `/invoices/{id}/payment-info`, `/payments` | **FAIL → PASS** | Thiếu implementation của `TenantPortalProvider` ở Backend khiến mọi API Tenant trả rỗng; `Constants.USE_MOCK_TENANT_DATA = true` ép app hiển thị dữ liệu giả. | Triển khai `TenantPortalProviderImpl` truy vấn dữ liệu thật từ MySQL (`InvoiceRepository`, `PaymentRepository`, `RoomRepository`), chuyển `USE_MOCK_TENANT_DATA = false`. |
| **Sự cố, Thông báo, Hồ sơ (GĐ6)** | `IncidentListFragment`, `IncidentFormFragment`, `IncidentDetailFragment`, `NotificationFragment`, `ProfileFragment` | `/api/tenant/incidents`, `/api/notifications`, `/api/users/me` | **PARTIAL → PASS** | `NotificationFragment` bỏ qua click của `LANDLORD`; `ProfileFragment` thiếu nút Back khi `LANDLORD` truy cập và thiếu validate nhập liệu; `IncidentService` để `room = null`. | Bổ sung điều hướng thông báo cho Chủ nhà, gắn Back toolbar & validate `ProfileFragment`, gắn phòng cho báo cáo sự cố, seed sẵn sự cố & thông báo mẫu. |

---

## 3. Phân loại màn hình theo 5 trạng thái

- **Đủ (Hoạt động đúng với dữ liệu thật Backend/MySQL):**
  - Chung: `SplashFragment`, `LoginFragment`, `RegisterFragment`, `NotificationFragment`, `ProfileFragment`.
  - Chủ nhà: `LandlordHomeFragment`, `RoomListFragment`, `RoomDetailFragment`, `RoomFormFragment`, `PropertySelectionFragment`, `TenantListFragment`, `TenantDetailFragment`, `TenantFormFragment`, `InvoiceFragment`, `InvoiceDetailFragment`, `UtilityFragment`, `DebtFragment`, `HealthScoreFragment`.
  - Người thuê: `TenantHomeFragment`, `MyRoomFragment`, `MyContractFragment`, `TenantInvoiceFragment`, `TenantInvoiceDetailFragment`, `QRPaymentFragment`, `PaymentHistoryFragment`, `IncidentListFragment`, `IncidentFormFragment`, `IncidentDetailFragment`.
- **Màn hình chết đã được hợp nhất / khôi phục:**
  - `LandlordHomeFragment` trước đó không thể truy cập vì `nav_graph.xml` trỏ vào `LandlordDashboardFragment` bản cũ → Đã gắn `landlordDashboardFragment` trực tiếp tới `LandlordHomeFragment`.
  - `TenantFormFragment` không có nút mở từ `TenantListFragment` / `TenantDetailFragment` → Đã nối FAB thêm mới và nút Chỉnh sửa.
  - `LandlordDashboardFragment` và `TenantDashboardFragment` được giữ lại làm vỏ tương thích cũ, không còn là điểm đến chính trên `nav_graph.xml`.
- **Chưa hoàn chỉnh / Giới hạn phạm vi nhóm:**
  - `ForgotPasswordFragment`: gọi `POST /api/auth/forgot-password` trả `200 OK`, Backend vẫn ở mức placeholder (chưa tích hợp SMTP/Email service).
  - Bộ màn hình `ContractFragment`, `ContractDetailFragment`, `ContractFormFragment` phía Chủ nhà và màn hình Chủ nhà cập nhật tiến độ sự cố (`/api/landlord/incidents`): chưa có bảng `contracts` và endpoint tương ứng trong phạm vi code nhóm hiện tại.

---

## 4. Danh sách lỗi đã phát hiện & khắc phục theo mức độ ưu tiên

### P0 – Critical
1. **Mất Dashboard Chủ nhà GĐ4.6 sau merge (`49adb8f`):** `nav_graph.xml` mở `LandlordDashboardFragment` cũ chỉ có 2 thẻ trống → Đã cập nhật `nav_graph.xml` trỏ `landlordDashboardFragment` sang `LandlordHomeFragment`.
2. **Luồng Người thuê chạy dữ liệu giả, Backend thiếu `TenantPortalProvider`:** Đã tạo `TenantPortalProviderImpl.java` kết nối `InvoiceRepository`, `PaymentRepository`, `RoomRepository`, `NotificationService` và tắt `Constants.USE_MOCK_TENANT_DATA = false`.
3. **Không thể đăng nhập tài khoản mẫu `123456`:** Hash mẫu trong `roomcare_phase2.sql` không khớp BCrypt của `"123456"` → Đã sửa hash chuẩn trong `roomcare_phase2.sql`, bổ sung tự sửa trong `DataSeeder.java` và chuẩn hóa DB MySQL local.
4. **Thông tin nhạy cảm / BOM trong cấu hình Backend:** Đã xóa BOM và dùng biến môi trường mặc định `${DB_PASSWORD:}` / `${JWT_SECRET:...}` trong `application.properties`.

### P1 – High
5. **Lỗi HTTP 204 khi xóa phòng/khách thuê & nuốt thông báo lỗi Backend:** `RoomController` và `TenantController` trả `Void` `204`; Android `RoomRepository`/`TenantRepository`/`AuthRepository`/`PropertyRepository` báo lỗi chung `HTTP 400` hoặc `Lỗi máy chủ` → Đã chuyển sang `ApiResponse<Void>` và dùng `ApiResultHandler.extractErrorMessage`.
6. **Gán cứng `propertyId = 1L` ở `TenantFormFragment`:** Khi Chủ nhà khác (`landlord2` hoặc Chủ nhà mới đăng ký) thêm khách thuê sẽ bị lỗi phân quyền → Đã tự động tải danh sách nhà trọ của Chủ nhà qua `PropertyViewModel`.
7. **Đăng ký chưa hỗ trợ chọn vai trò Chủ nhà / Người thuê:** Đã cập nhật `RegisterRequest`, `AuthService`, `RegisterFragment` và `fragment_register.xml`.
8. **Chưa lọc/tìm kiếm ở `RoomListFragment`, thiếu Thêm/Sửa/Xóa ở `TenantListFragment` & `TenantDetailFragment`:** Đã bổ sung đầy đủ và kiểm tra điều kiện xóa an toàn ở Backend.
9. **Chưa hiển thị `currentTenant` ở phòng & `currentRoom` ở khách thuê:** Đã tổng hợp từ `InvoiceRepository` trong `RoomService` và `TenantService`.

### P2 – Medium
10. **Xung đột 2 hệ màu (Tím mặc định vs RoomCare Teal) và icon hệ thống cũ:** Đã đồng bộ `themes.xml`, `colors.xml` và thay bằng bộ vector `ic_*`.
11. **Hộp thoại xác nhận hiển thị lẫn nút tiếng Anh (`Cancel`):** Đã đổi toàn bộ `android.R.string.cancel` sang `R.string.cancel` (`"Hủy"`).
12. **Lỗi hiển thị ký tự `?` tiếng Việt trong dữ liệu mẫu MySQL và thiếu seed `incidents`/`notifications` khi DB đã có `users`:** Đã bổ sung xử lý tự chuẩn hóa trong `DataSeeder.java` và cập nhật UTF-8 trên DB local.

---

## 5. Kết quả kiểm thử thực tế (Đã chạy sau chỉnh sửa cuối cùng)

### 5.1. Build & Unit/Integration Test tự động
- **Backend Maven (`mvn test -f backend/pom.xml`, JDK 21):**
  - `FinanceAndHealthScoreTest`: **8/8 PASS** (0 Failures, 0 Errors, 0 Skipped), bao gồm kiểm thử tạo hóa đơn, tránh trùng kỳ, tính điện/nước `BigDecimal`, thanh toán từng phần/đủ/chống trùng, công nợ, Health Score (Tốt/Theo dõi/Rủi ro/Thiếu dữ liệu) và kiểm thử liên vai trò `tenantPortalProvider_scopesDataAndNeverMarksPaidOnQrView` (xác nhận mở QR không đổi trạng thái `UNPAID`, chặn đọc chéo hóa đơn giữa hai Người thuê, gửi thông báo gia hạn tới Chủ nhà).
- **Android Gradle (`.\gradlew.bat installDebug testDebugUnitTest`, JDK 17):**
  - **BUILD SUCCESSFUL**
  - `ExampleUnitTest` + `FinanceHealthScoreUnitTest`: **5/5 PASS** (0 Failures, 0 Errors, 0 Skipped).

### 5.2. Kiểm thử API & Phân quyền trực tiếp (`http://localhost:8080`)
- Đăng nhập `landlord1@example.com` (`LANDLORD`): `/api/landlord/dashboard` trả `5` phòng, doanh thu `23.752.500 ₫`, tỷ lệ thu `57.4%`, Health Score trung bình `74/100`; `/api/landlord/debts` trả tổng nợ `10.115.000 ₫`; Phòng `102` hiển thị `currentTenant = Lê Văn C (0923456789)`; Người thuê `Lê Văn C` hiển thị `currentRoom = Phòng 201`.
- Đăng nhập `tenant1@example.com` (`TENANT`): `/api/tenant/dashboard` trả `Lê Văn C`, phòng `201`, số tiền cần thanh toán `5.065.000 ₫`, hạn `26/11/2026`, `2` thông báo chưa đọc; `/api/tenant/my-room` trả phòng `201`, chủ nhà `Nguyễn Văn A`, SĐT `0901234567`; `/api/tenant/my-contract` trả `ACTIVE`, `2.700.000 ₫`; `/api/tenant/invoices` trả `5` hóa đơn; mở `/api/tenant/invoices/{id}/payment-info` xác nhận `INV-202611-201-R3` vẫn giữ nguyên trạng thái `UNPAID`; `/api/tenant/payments` trả `4` giao dịch; `/api/tenant/incidents` trả `2` sự cố.
- Đăng ký mới qua `POST /api/auth/register`: kiểm thử thành công cho cả `TENANT` và `LANDLORD` (Chủ nhà mới tự động có `1` `Property` mặc định; tài khoản test tạm đã được xóa sạch sau khi test).
- Phân quyền bảo mật:
  - `TENANT` gọi `/api/landlord/dashboard` → **HTTP 403** (PASS)
  - `LANDLORD` gọi `/api/tenant/dashboard` → **HTTP 403** (PASS)
  - `landlord2@example.com` đọc hóa đơn của `landlord1` → **HTTP 400** chặn truy cập (PASS)
  - Người thuê khác đọc hóa đơn/QR của `tenant1` → bị chặn (PASS trong unit/integration test)

### 5.3. Kiểm thử UI/E2E trên Android Emulator (`Medium_Phone_API_37.0`)
- **Luồng Chủ nhà:** Đăng nhập `landlord1@example.com` → `LandlordHomeFragment` → `NotificationFragment` (hiển thị 2 thông báo sự cố mới) → `RoomListFragment` + lọc Chip `Đã thuê` (lọc đúng phòng `102`) → `TenantListFragment` (có nút FAB thêm mới) → `TenantDetailFragment` (có Sửa/Xóa) → `ProfileFragment` → Xác nhận hộp thoại Đăng xuất (`Hủy` / `Đăng xuất`) → quay về `LoginFragment`.
- **Luồng Người thuê & Giữ phiên:** Đăng nhập `tenant1@example.com` → tắt app (`am force-stop`) và mở lại để xác nhận `SplashFragment` tự khôi phục vào `TenantHomeFragment` → `MyRoomFragment` → `MyContractFragment` → `TenantInvoiceFragment` → `PaymentHistoryFragment` → `TenantInvoiceDetailFragment` → `QRPaymentFragment` → quay lại xác nhận hóa đơn vẫn `Chưa thanh toán` → `IncidentListFragment` → `IncidentDetailFragment`.
- **`adb logcat`:** Không ghi nhận bất kỳ `FATAL EXCEPTION` nào trong suốt quá trình kiểm thử.

---

## 6. Giới hạn còn tồn tại

- Không có quyền truy cập trực tiếp file thiết kế Figma trên cloud (`figma.com`) từ môi trường agent; toàn bộ đối chiếu giao diện được thực hiện dựa trên tài liệu Word yêu cầu nghiệp vụ và design system `roomcare_ui.xml` mà nhóm đã dựng trong repo.
- Luồng **Quên mật khẩu** (`ForgotPasswordFragment` / `/api/auth/forgot-password`) chưa có dịch vụ gửi email/OTP thật ở Backend.
- Chức năng **Quản lý hợp đồng CRUD phía Chủ nhà** (`contracts` table riêng và `ContractFragment` / `ContractFormFragment`) chưa nằm trong codebase nhóm hiện tại nên chỉ dừng ở mức tổng hợp trạng thái hợp đồng đang thuê và luồng gửi yêu cầu gia hạn của Người thuê.
