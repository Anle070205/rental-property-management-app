# Báo cáo Tổng kết Module Thành viên 2 (GĐ4.1 – GĐ4.6)

- **Thành viên phụ trách:** `tranthanhtai_23115053122139`
- **Nhánh Git:** `feature/tranthanhtai_23115053122139/finance-health-score` (tạo từ `develop` tại `75bc646`)
- **Trạng thái:** Hoàn thành GĐ4.1 – GĐ4.6, đã kiểm thử Unit Test (Backend & Android) và tích hợp API thực tế với MySQL.

---

## 1. Tổng hợp 6 giai đoạn đã triển khai

### GĐ4.1 – Danh sách & Tạo hóa đơn (`InvoiceFragment`)
- **Backend:** `Invoice`, `InvoiceStatus` (`UNPAID`, `PARTIALLY_PAID`, `PAID`, `OVERDUE`), `InvoiceRepository`, `InvoiceService`, `InvoiceController`.
  - `GET /api/landlord/invoices?month=&roomId=&propertyId=&status=`
  - `POST /api/landlord/invoices` (tạo cho 1 phòng, tự động điền tiền phòng từ `Room.rentPrice`, tự liên kết `UtilityRecord` kỳ tương ứng nếu có, chặn tạo trùng `(roomId, billingMonth)`).
  - `POST /api/landlord/invoices/batch` (tạo hàng loạt theo nhà trọ hoặc danh sách phòng, bỏ qua phòng đã có hóa đơn kỳ đó và trả về danh sách lý do bỏ qua).
- **Android:** `InvoiceModels.kt`, `InvoiceRepository.kt`, `InvoiceViewModel.kt`, `InvoiceAdapter.kt`, `InvoiceFragment.kt`, `fragment_invoice.xml`, `item_invoice.xml`, `dialog_create_invoice.xml`, `dialog_batch_create_invoice.xml`.
  - Hỗ trợ lọc theo tháng, phòng, trạng thái; tính tổng tiền bằng `BigDecimal`; hiển thị bảng tạm tính trực tiếp khi nhập.

### GĐ4.2 – Điện, nước & Dịch vụ (`UtilityFragment`)
- **Backend:** `UtilityRecord`, `UtilityRecordRepository`, `UtilityService`, `UtilityController`.
  - `GET /api/landlord/utilities?month=&roomId=&propertyId=`
  - `GET /api/landlord/utilities/previous-index?roomId=&month=` (gợi ý chỉ số cũ và đơn giá từ kỳ gần nhất).
  - `POST /api/landlord/utilities` & `PUT /api/landlord/utilities/{id}`.
  - Chặn chỉ số mới nhỏ hơn chỉ số cũ trừ khi bật chế độ điều chỉnh (`isAdjustment = true`) và nhập `adjustmentReason`.
  - Lưu vết `historyLog` mỗi lần tạo/chỉnh sửa; chặn chỉnh sửa khi hóa đơn kỳ đó đã chốt (`PAID`); tự động đồng bộ lại số tiền trên hóa đơn chưa chốt để không lệch dữ liệu giữa hai nơi.
- **Android:** `UtilityModels.kt`, `UtilityRepository.kt`, `UtilityViewModel.kt`, `UtilityRecordAdapter.kt`, `UtilityFragment.kt`, `fragment_utility.xml`, `item_utility_record.xml`.

### GĐ4.3 – Chi tiết hóa đơn & Theo dõi thanh toán (`InvoiceDetailFragment`)
- **Backend:** `Payment`, `PaymentMethod` (`CASH`, `BANK_TRANSFER`), `PaymentRepository`, `CreatePaymentRequest`, `PaymentResponse`, mở rộng `InvoiceService` & `InvoiceController`.
  - `GET /api/landlord/invoices/{id}`
  - `POST /api/landlord/invoices/{id}/payments`
  - Chỉ ghi nhận thanh toán khi `confirmedByLandlord == true` (không tự chốt chỉ vì mở màn hình QR); kiểm tra số tiền `> 0` và `<= remainingAmount`; chống trùng `transactionCode` và trùng giao dịch vừa gửi trong 2 phút.
- **Android:** `PaymentHistoryAdapter.kt`, `InvoiceDetailFragment.kt`, `fragment_invoice_detail.xml`, `item_payment_history.xml`, `dialog_record_payment.xml`.

### GĐ4.4 – Quản lý công nợ (`DebtFragment`)
- **Backend:** `DebtSummaryResponse`, `RoomTenantDebtResponse`, `DebtHistoryItemResponse`, `DebtService`, `DebtController`.
  - `GET /api/landlord/debts?roomId=&tenantId=&month=&overdueLevel=&keyword=`
  - `GET /api/landlord/debts/{tenantId}`
  - Tính trực tiếp từ `invoices` và `payments` thực tế (không tạo bảng độc lập), xử lý thanh toán một phần/bổ sung/giảm trừ và dựng lịch sử biến động công nợ theo thứ tự thời gian.
- **Android:** `DebtModels.kt`, `DebtRepository.kt`, `DebtViewModel.kt`, `DebtAdapter.kt`, `DebtFragment.kt`, `fragment_debt.xml`, `item_debt.xml`.
  - Cho phép mở nhanh chi tiết hóa đơn, xem biến động công nợ, và gọi điện hoặc mở hồ sơ người thuê (`TenantDetailFragment`).

### GĐ4.5 – Health Score phòng (`HealthScoreFragment`)
- **Backend:** `HealthScoreOverviewResponse`, `RoomHealthScoreResponse`, `HealthFactorResponse`, `HealthScoreService`, `HealthScoreController`.
  - `GET /api/landlord/health-scores?ratingLevel=&propertyId=`
  - `GET /api/landlord/rooms/{id}/health-score`
- **Công thức đề xuất (chờ nhóm thống nhất):** Thang điểm `0–100` gồm 4 trụ cột:
  1. **Thanh toán đúng hạn (40đ / 40%):** Khởi điểm 40đ; `-12đ`/hóa đơn quá hạn, `-5đ`/hóa đơn trả một phần, `-8đ` nếu trễ > 15 ngày, `-2đ`/hóa đơn chưa thu trong hạn. Thiếu dữ liệu hóa đơn áp dụng điểm cơ sở `35/40`.
  2. **Hợp đồng & lấp đầy (25đ / 25%):** `OCCUPIED` = 25đ, `EXPIRING_SOON` = 15đ, `VACANT` = 12đ, `NEEDS_ATTENTION` = 8đ.
  3. **Ổn định điện nước (20đ / 20%):** Phòng đang thuê chưa ghi chỉ số kỳ hiện tại `-8đ`; tiêu thụ tăng đột biến > 50% `-6đ`; điều chỉnh đồng hồ `-4đ`. Thiếu dữ liệu áp dụng điểm cơ sở `16/20`.
  4. **Sự cố & vận hành (15đ / 15%):** `NEEDS_ATTENTION` `-10đ`; quá hạn từ 2 kỳ trở lên `-5đ`.
  - Phân hạng: `GOOD` (`80–100`), `WARNING` (`50–79`), `RISK` (`0–49`), kèm lý do trừ điểm và gợi ý hành động cải thiện.
- **Android:** `HealthScoreModels.kt`, `HealthScoreRepository.kt`, `HealthScoreViewModel.kt`, `HealthScoreAdapter.kt`, `HealthScoreFragment.kt`, `fragment_health_score.xml`, `item_health_score.xml`, tích hợp nút xem Health Score trong `RoomDetailFragment`.

### GĐ4.6 – Dashboard Chủ nhà (`LandlordHomeFragment` & `LandlordDashboardFragment`)
- **Backend:** `LandlordDashboardResponse`, `MonthlyRevenueResponse`, `AttentionItemResponse`, `DashboardService`, `DashboardController`.
  - `GET /api/landlord/dashboard`
- **Android:** `DashboardModels.kt`, `DashboardRepository.kt`, `DashboardViewModel.kt`, `MonthlyRevenueChartView.kt`, `LandlordHomeFragment.kt`, `fragment_landlord_home.xml`, `LandlordDashboardFragment.kt`, `fragment_landlord_dashboard.xml`.
  - Hiển thị thống kê phòng, doanh thu/đã thu/chưa thu, tỷ lệ thu tiền, biểu đồ doanh thu theo tháng, hợp đồng sắp hết hạn, phòng cần chú ý và điều hướng tới 6 phân hệ.

---

## 2. Sửa lỗi tích hợp Phase 2 & Dữ liệu mẫu
- Khắc phục các lỗi biên dịch tồn đọng trong `PropertyViewModel.kt`, `PropertySelectionFragment.kt`, `RoomDetailFragment.kt`, `RoomListFragment.kt`, `TenantListFragment.kt`, `TenantDetailFragment.kt`, `TenantFormFragment.kt`.
- Bổ sung `DataSeeder.java` (tự seed bảng `utility_records`, `invoices`, `payments` nếu bảng `invoices` đang trống) và script `backend/database/roomcare_phase4_member2.sql`.
- Lưu ý cấu hình mạng Android (`Constants.kt`): `BASE_URL` đặt mặc định `http://10.0.2.2:8080/` cho Android Emulator (có thể đổi sang IP LAN khi chạy máy thật; đã thêm `192.168.1.141` vào `network_security_config.xml`).
- Lưu ý Figma: Môi trường thực thi không truy cập trực tiếp được file Figma, giao diện bám theo hệ thống màu sắc (`colors.xml`), kích thước (`dimens.xml`) và Material 3 XML của project hiện có — cần đối chiếu lại chi tiết với Figma khi review nhóm.

---

## 3. Kết quả Build & Kiểm thử thực tế

1. **Backend Unit Tests (`FinanceAndHealthScoreTest.java`):**
   - Lệnh: `mvnw.cmd test -f pom.xml`
   - Kết quả: `Tests run: 7, Failures: 0, Errors: 0, Skipped: 0` (`BUILD SUCCESS`).
2. **Android Build & Unit Tests (`FinanceHealthScoreUnitTest.kt` + `ExampleUnitTest.kt`):**
   - Lệnh: `.\gradlew.bat assembleDebug testDebugUnitTest --console=plain`
   - Kết quả: `BUILD SUCCESSFUL`, 5/5 unit tests PASS (`ExampleUnitTest`: 1/1, `FinanceHealthScoreUnitTest`: 4/4).
3. **Kiểm thử tích hợp API thực tế với MySQL (`RoomCare`):**
   - Tài khoản test: `landlord1@example.com` / `123456`.
   - `GET /api/landlord/invoices` → 7 hóa đơn.
   - `GET /api/landlord/utilities` → 7 bản ghi điện nước.
   - `GET /api/landlord/debts` → Tổng nợ `10.115.000 ₫`, 2 hóa đơn quá hạn tại 3 phòng.
   - `GET /api/landlord/health-scores` → Điểm TB `74/100` (2 phòng Tốt, 2 phòng Cần theo dõi, 1 phòng Rủi ro).
   - `GET /api/landlord/dashboard` → 5 phòng, tổng doanh thu `23.752.500 ₫`, đã thu `13.637.500 ₫`, tỷ lệ thu `57.4%` (khớp tuyệt đối phần chưa thu = `10.115.000 ₫`).
   - Đã kiểm chứng chặn tạo trùng hóa đơn cùng phòng/kỳ và chặn truy cập chéo giữa `landlord2` và `landlord1`.

---

## 4. Lịch sử Commit trên nhánh Feature

1. `c7ee9b3` – `docs(gd4): phân tích cấu trúc và kế hoạch module thành viên 2`
2. `3a143bb` – `feat(gd4.1): xây dựng danh sách hóa đơn`
3. `8e25321` – `feat(gd4.1): bổ sung tạo hóa đơn và kiểm tra dữ liệu`
4. `fb2ced2` – `feat(gd4.2): xây dựng giao diện ghi điện nước`
5. `ccdfb6a` – `feat(gd4.2): tính chi phí dịch vụ và tích hợp API`
6. `82c92b6` – `feat(gd4.3): hiển thị chi tiết hóa đơn`
7. `041666d` – `feat(gd4.3): ghi nhận và theo dõi thanh toán`
8. `dc0bf9c` – `feat(gd4.4): tổng hợp công nợ và bộ lọc`
9. `509d5c2` – `feat(gd4.5): tích hợp và hiển thị Health Score`
10. `f86ca1c` – `feat(gd4.6): xây dựng dashboard chủ nhà`
11. `982ed6b` – `test(gd4): bổ sung kiểm thử và sửa lỗi tích hợp`
12. Commit hiện tại – `docs(gd4): cập nhật hướng dẫn và báo cáo kết quả`

---

## 5. Hướng dẫn Demo Bảo vệ Bài tập

1. **Khởi động MySQL & Backend:**
   - Mở MySQL (`RoomCare` database, mặc định `backend/src/main/resources/application.properties`).
   - Khởi chạy Spring Boot trên cổng `8080`. `DataSeeder` tự động tạo dữ liệu mẫu khi bảng `invoices` còn trống.
2. **Chạy App trên Android Studio:**
   - Mở thư mục `frontend` bằng Android Studio, chọn Android Emulator (dùng `10.0.2.2:8080`) và nhấn Run.
   - Đăng nhập bằng `landlord1@example.com` / `123456`.
3. **Kịch bản Demo 6 màn hình:**
   - **Landlord Home (`GĐ4.6`):** Xem KPI 5 phòng, tổng doanh thu, tỷ lệ thu, biểu đồ cột doanh thu theo tháng, danh sách phòng sắp hết hạn (`201`) & phòng cần chú ý (`2B`).
   - **Điện, nước & dịch vụ (`GĐ4.2`):** Chọn phòng trống `101`, nhập chỉ số mới, kiểm tra bảng tính trước khi lưu và thử nhập chỉ số mới < chỉ số cũ để kiểm tra cảnh báo; sửa 1 bản ghi chưa chốt (kỳ hóa đơn `UNPAID`) và thấy hóa đơn tự động đồng bộ.
   - **Hóa đơn (`GĐ4.1`):** Lọc theo kỳ/phòng/trạng thái; tạo hóa đơn cho `101` (tự lấy giá phòng & điện nước vừa ghi) và thử tạo trùng kỳ để kiểm tra thông báo lỗi; thử tạo hàng loạt.
   - **Chi tiết hóa đơn & Thanh toán (`GĐ4.3`):** Mở hóa đơn phòng `102` (đang `PARTIALLY_PAID`), xem lịch sử thanh toán đợt 1, bấm `Xác nhận ghi nhận thanh toán` phần còn lại → trạng thái chuyển thành `PAID`.
   - **Công nợ (`GĐ4.4`):** Lọc phòng `2B` (`OVERDUE_30_PLUS`), xem lịch sử biến động công nợ, mở nhanh hóa đơn hoặc bấm `Liên hệ`.
   - **Health Score (`GĐ4.5`):** Xem điểm `102` (Tốt), `201` (Cần theo dõi), `2B` (Rủi ro) cùng giải thích 4 yếu tố và gợi ý cải thiện; quay lại Dashboard kiểm tra số liệu cập nhật đồng bộ.
