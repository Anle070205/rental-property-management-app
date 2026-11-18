# Kế hoạch Phân tích & Thiết kế Module Thành viên 2 (GĐ4.1 – GĐ4.6)

- **Thành viên phụ trách:** `tranthanhtai_23115053122139`
- **Nhánh làm việc:** `feature/tranthanhtai_23115053122139/finance-health-score` (tạo từ `develop` tại commit `75bc646`)
- **Phạm vi:** Tài chính (Hóa đơn, Điện nước & Dịch vụ, Chi tiết & Thanh toán, Công nợ), Health Score phòng và Dashboard Chủ nhà.

---

## 1. Báo cáo khảo sát Source Code hiện tại

### 1.1 Backend (`backend/`)
- **Tech stack:** Spring Boot 3.2.0, Java 17, Spring Security + JWT (`JwtAuthenticationFilter`, `JwtUtil`), Spring Data JPA (Hibernate `ddl-auto=update`), MySQL 8 (`RoomCare`).
- **Đã triển khai:**
  - **Phase 1 (Auth):** `User`, `Role` (`LANDLORD`, `TENANT`), `AuthController`, `AuthService` (`/api/auth/**`).
  - **Phase 2 (Landlord CRUD):** `Property`, `Room`, `RoomStatus` (`VACANT`, `OCCUPIED`, `EXPIRING_SOON`, `NEEDS_ATTENTION`), `Tenant`, `PropertyController`, `RoomController`, `TenantController`, `GlobalExceptionHandler`, `DataSeeder`.
  - **Phân quyền sẵn sàng:** `SecurityConfig` đã bảo vệ `/api/landlord/**` với `hasRole("LANDLORD")` và tiêm `@AuthenticationPrincipal User landlord` vào các controller.
- **Chưa triển khai:**
  - Chưa có module Hợp đồng (`Contract`) và Sự cố/Bảo trì (`Maintenance`/`Incident`) của Phase 3 (các field `currentContract`, `currentTenant` trong `RoomResponse` và `TenantResponse` đang để `TODO: Phase 3`).
  - Chưa có các entity, repository, service, controller cho Hóa đơn (GĐ4.1), Điện nước (GĐ4.2), Thanh toán (GĐ4.3), Công nợ (GĐ4.4), Health Score (GĐ4.5) và Dashboard Chủ nhà (GĐ4.6).

### 1.2 Android Frontend (`frontend/`)
- **Tech stack:** Kotlin, XML Layout + ViewBinding (không Compose, không DataBinding), MVVM (`StateFlow` + `Resource<T>`), Navigation Component + SafeArgs, Retrofit2 + OkHttp3 + Gson, Material Design 3 (`Theme.Material3.DayNight.NoActionBar`).
- **API & Utils có thể tái sử dụng:**
  - `ApiService.kt`, `RetrofitClient.kt` (tự động gắn JWT `Bearer` header), `PageResponse<T>`, `ApiResponse<T>`.
  - `PropertyRepository`, `RoomRepository`, `TenantRepository`, `PropertyResponse`, `RoomResponse`, `TenantResponse`.
  - `CurrencyUtils.kt` (định dạng VND), `DateUtils.kt`, `ValidationUtils.kt`, `Extensions.kt`, `SharedPreferencesManager.kt`.
  - Tài nguyên chung: `colors.xml` (màu trạng thái `status_vacant`, `status_occupied`, `status_expiring_soon`, `status_needs_attention`, `error`, `success`, `warning`, `info`), `dimens.xml`, `strings.xml`, 18 vector drawables (`ic_money`, `ic_room`, `ic_people`, `ic_calendar`, ...).
- **Lỗi biên dịch tồn đọng từ Phase 2 (`origin/develop` `75bc646`):**
  - Đã ghi nhận trong `frontend/PHASE2_ANDROID_STATUS.md` gồm: `RoomDetailFragment.kt`, `TenantDetailFragment.kt`, `RoomListFragment.kt`, `TenantFormFragment.kt`, `PropertySelectionFragment.kt` (sai tên package/biến giữa `Resource` và State data class, sai field `roomId`/`idCard`/`moveInDate`/`notes`). Sẽ khắc phục dứt điểm để toàn bộ app build thành công mà không phá vỡ luồng của thành viên khác.

### 1.3 Thiết kế UI/UX & Giới hạn truy cập Figma
- **Giới hạn:** Môi trường thực thi không truy cập trực tiếp được file thiết kế trên Figma (`https://www.figma.com/design/L9hS7O7oLIp7iqyrwmr6Nq/LTTD_QLNhaTro?node-id=0-1`).
- **Giải pháp áp dụng:** Giữ nguyên ngôn ngữ thiết kế Material Design 3 + XML Layout + hệ thống màu sắc (`colors.xml`), khoảng cách (`dimens.xml`), kiểu chữ và thẻ Card (`card_corner_radius = 12dp`, `card_elevation = 4dp`, `TextInputLayout.OutlinedBox`) đang dùng thống nhất trong project. Các màn hình mới cần đối chiếu lại chi tiết với bản Figma khi review nhóm.

---

## 2. Thiết kế Cơ sở dữ liệu & API (Backend)

Tất cả giá trị tiền tệ và chỉ số đo lường sử dụng `BigDecimal` / `DECIMAL(15,2)` (hoặc `DECIMAL(12,2)`) để đảm bảo độ chính xác tài chính. Mọi endpoint thuộc `/api/landlord/**` đều kiểm tra quyền sở hữu `landlord.getId()`.

### 2.1 Bảng mới bổ sung
1. **`utility_records`** (GĐ4.2):
   - `id`, `landlord_id`, `property_id`, `room_id`, `billing_month` (`YYYY-MM`, unique theo `(room_id, billing_month)`).
   - `prev_electric_index`, `curr_electric_index`, `electric_usage`, `electric_unit_price`, `electricity_amount`.
   - `prev_water_index`, `curr_water_index`, `water_usage`, `water_unit_price`, `water_amount`.
   - `internet_fee`, `service_fee`, `total_utility_amount`.
   - `is_adjustment` (`BOOLEAN`), `adjustment_reason` (`TEXT`), `notes` (`TEXT`), `history_log` (`TEXT` - lưu vết thay đổi khi tạo/cập nhật).
   - `created_at`, `updated_at`.
2. **`invoices`** (GĐ4.1 & GĐ4.3):
   - `id`, `invoice_code` (duy nhất, VD `INV-202611-102`), `landlord_id`, `property_id`, `room_id`, `tenant_id` (nullable), `utility_record_id` (nullable).
   - `billing_month` (`YYYY-MM`, unique theo `(room_id, billing_month)` tránh tạo trùng hóa đơn).
   - `room_rent`, `electricity_amount`, `water_amount`, `internet_amount`, `service_amount`, `other_fees`, `previous_debt`, `discount_amount`, `total_amount`, `paid_amount`, `remaining_amount`.
   - `status` (`UNPAID`, `PARTIALLY_PAID`, `PAID`, `OVERDUE`), `due_date`, `notes`, `created_at`, `updated_at`.
3. **`payments`** (GĐ4.3):
   - `id`, `invoice_id`, `landlord_id`, `amount`, `payment_method` (`CASH`, `BANK_TRANSFER`), `transaction_code`, `receipt_note`, `paid_at`, `created_at`.

### 2.2 Danh sách API Endpoints (`/api/landlord/**`)
- **GĐ4.1 Hóa đơn:**
  - `GET /api/landlord/invoices?month=&roomId=&propertyId=&status=` – Danh sách hóa đơn + tổng hợp nhanh.
  - `POST /api/landlord/invoices` – Tạo hóa đơn cho 1 phòng (tự động điền tiền phòng, liên kết điện nước kỳ đó nếu có, cộng nợ kỳ trước).
  - `POST /api/landlord/invoices/batch` – Tạo hàng loạt hóa đơn cho nhiều phòng theo kỳ.
- **GĐ4.2 Điện, nước & dịch vụ:**
  - `GET /api/landlord/utilities?month=&roomId=&propertyId=` – Danh sách chỉ số điện nước.
  - `GET /api/landlord/utilities/previous-index?roomId=&month=` – Gợi ý chỉ số cũ từ kỳ trước.
  - `POST /api/landlord/utilities` – Ghi chỉ số mới & tính tiền.
  - `PUT /api/landlord/utilities/{id}` – Chỉnh sửa khi hóa đơn kỳ đó chưa chốt (`PAID`), tự động đồng bộ lại số tiền trên hóa đơn liên kết và ghi log lịch sử.
- **GĐ4.3 Chi tiết hóa đơn & Thanh toán:**
  - `GET /api/landlord/invoices/{id}` – Chi tiết hóa đơn, chỉ số điện nước kèm theo và lịch sử thanh toán.
  - `POST /api/landlord/invoices/{id}/payments` – Xác nhận thu tiền mặt/chuyển khoản từ Chủ nhà, chống trùng giao dịch, cập nhật `paidAmount`, `remainingAmount`, `status`.
- **GĐ4.4 Công nợ:**
  - `GET /api/landlord/debts?roomId=&tenantId=&month=&overdueLevel=&keyword=` – Tổng hợp công nợ thực tế tính trực tiếp từ hóa đơn & thanh toán.
  - `GET /api/landlord/debts/{tenantId}` – Chi tiết công nợ & lịch sử biến động theo người thuê.
- **GĐ4.5 Health Score:**
  - `GET /api/landlord/health-scores` – Danh sách điểm Health Score của tất cả phòng thuộc Chủ nhà.
  - `GET /api/landlord/rooms/{id}/health-score` – Chi tiết Health Score của một phòng.
- **GĐ4.6 Dashboard Chủ nhà:**
  - `GET /api/landlord/dashboard` – Số liệu tổng quan phòng, doanh thu/đã thu/chưa thu, hợp đồng sắp hết hạn, phòng cần chú ý và doanh thu theo tháng.

---

## 3. Đề xuất Công thức tính Health Score phòng (GĐ4.5)

> **Lưu ý quan trọng:** Do hệ thống hiện chưa có công thức/API Health Score được nhóm định nghĩa trước và chưa có bảng Hợp đồng/Bảo trì (thuộc Phase 3), dưới đây là **công thức đề xuất** dựa trên dữ liệu thực tế hiện có (`RoomStatus`, `Invoice`, `Payment`, `UtilityRecord`) để nhóm xem xét, thống nhất hoặc điều chỉnh trọng số khi tích hợp đầy đủ với module của các thành viên khác.

**Thang điểm tổng: 0 – 100 điểm**, gồm 4 trụ cột:

1. **Lịch sử thanh toán đúng hạn (tối đa 40 điểm):**
   - Khởi điểm: `40` điểm.
   - Mỗi hóa đơn `OVERDUE` (quá hạn): `-12` điểm.
   - Mỗi hóa đơn `PARTIALLY_PAID` (chưa thanh toán đủ): `-5` điểm.
   - Có hóa đơn trễ hạn > 15 ngày: trừ thêm `-8` điểm.
   - Có hóa đơn `UNPAID` chưa quá hạn: `-2` điểm/hóa đơn.
   - *Quy tắc xử lý dữ liệu thiếu:* Nếu phòng chưa từng phát sinh hóa đơn, gán mức cơ sở trung lập-tốt `35/40` điểm (tránh phạt phòng mới tạo nhưng phân biệt với phòng đã thanh toán tốt nhiều kỳ `40/40`).

2. **Tình trạng hợp đồng & khai thác phòng (tối đa 25 điểm):**
   - `OCCUPIED` (đang thuê ổn định): `25/25` điểm.
   - `EXPIRING_SOON` (sắp hết hạn hợp đồng): `15/25` điểm (`-10` điểm rủi ro trống phòng).
   - `VACANT` (đang trống): `12/25` điểm (`-13` điểm do chưa có doanh thu).
   - `NEEDS_ATTENTION` (cần chú ý): `8/25` điểm (`-17` điểm).

3. **Ổn định điện nước & ghi nhận định kỳ (tối đa 20 điểm):**
   - Khởi điểm: `20` điểm khi có ghi nhận đầy đủ và ổn định.
   - Phòng đang thuê (`OCCUPIED`/`EXPIRING_SOON`) nhưng kỳ hiện tại chưa ghi chỉ số điện nước: `-8` điểm.
   - Tiêu thụ điện hoặc nước tăng đột biến (`> 50%` so với kỳ liền trước): `-6` điểm.
   - Có bản ghi phải điều chỉnh đồng hồ (`isAdjustment = true`): `-4` điểm.
   - *Quy tắc xử lý dữ liệu thiếu:* Nếu phòng trống hoặc chưa có đủ 2 kỳ dữ liệu điện nước, gán `16/20` điểm cơ sở.

4. **Sự cố & Tín hiệu vận hành (tối đa 15 điểm):**
   - Khởi điểm: `15` điểm (bình thường).
   - Trạng thái phòng `NEEDS_ATTENTION`: `-10` điểm (dấu hiệu sự cố/bảo trì).
   - Có từ 2 hóa đơn quá hạn trở lên: `-5` điểm (rủi ro vận hành/nợ xấu kéo dài).

**Phân loại mức đánh giá:**
- **80 – 100 (`GOOD`):** Tốt – Vận hành ổn định.
- **50 – 79 (`WARNING`):** Cần theo dõi – Cần nhắc thanh toán, gia hạn hoặc kiểm tra định kỳ.
- **0 – 49 (`RISK`):** Rủi ro – Cần xử lý ngay công nợ quá hạn hoặc sự cố phòng.

**Xu hướng (`trend`):** So sánh trạng thái nợ và thanh toán kỳ mới nhất với kỳ trước (`UP`, `STABLE`, `DOWN`).

---

## 4. Kế hoạch triển khai & Lịch sử Commit dự kiến
1. `docs(gd4)`: Phân tích cấu trúc và kế hoạch module thành viên 2 (`PHASE4_MEMBER2_PLAN.md`).
2. `feat(gd4.1)`: Xây dựng danh sách hóa đơn (`Invoice`, `InvoiceRepository`, `InvoiceAdapter`, `fragment_invoice.xml`, `item_invoice.xml`).
3. `feat(gd4.1)`: Bổ sung tạo hóa đơn đơn lẻ/hàng loạt & kiểm tra dữ liệu (`InvoiceService`, `InvoiceController`, `InvoiceFragment`, `InvoiceViewModel`).
4. `feat(gd4.2)`: Xây dựng giao diện ghi điện nước (`UtilityModels`, `UtilityRecordAdapter`, `fragment_utility.xml`, `item_utility_record.xml`).
5. `feat(gd4.2)`: Tính chi phí dịch vụ & tích hợp API (`UtilityRecord`, `UtilityService`, `UtilityController`, `UtilityFragment`).
6. `feat(gd4.3)`: Hiển thị chi tiết hóa đơn (`PaymentHistoryAdapter`, `fragment_invoice_detail.xml`, `item_payment_history.xml`).
7. `feat(gd4.3)`: Ghi nhận & theo dõi thanh toán (`Payment`, `InvoiceDetailFragment`, `dialog_record_payment.xml`).
8. `feat(gd4.4)`: Tổng hợp công nợ & bộ lọc (`DebtService`, `DebtController`, `DebtFragment`).
9. `feat(gd4.5)`: Tích hợp & hiển thị Health Score (`HealthScoreService`, `HealthScoreController`, `HealthScoreFragment`).
10. `feat(gd4.6)`: Xây dựng Dashboard Chủ nhà (`DashboardService`, `DashboardController`, `LandlordHomeFragment`, `LandlordDashboardFragment`).
11. `test(gd4)`: Seed data, sửa lỗi biên dịch Phase 2 tồn đọng, chạy test Backend & Android.
12. `docs(gd4)`: Cập nhật báo cáo tổng kết & hướng dẫn demo (`PHASE4_MEMBER2_SUMMARY.md`).
