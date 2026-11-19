# ROOMCARE – BÁO CÁO RÀ SOÁT & NÂNG CẤP UI/UX (UI REVIEW)

## 1. Bối cảnh & Kiểm toán ban đầu

1. **Đứt gãy giao diện Dashboard Chủ nhà:** Do xung đột merge ở commit `49adb8f`, người dùng `LANDLORD` ban đầu bị đưa vào màn hình sơ khai, chưa có cấu trúc tổng quan và điều hướng thống nhất với luồng Người thuê.
2. **Xung đột hệ thống màu sắc & component:** Các module GĐ1–GĐ2 trước đây còn dùng màu Material mặc định (`purple_500`), bo góc chưa đồng nhất, thiếu hệ thống container trạng thái và icon box theo thiết kế.
3. **Thiếu đồng bộ với Figma & điều hướng:**
   - Bảng màu, bo góc nút/input (`12dp`), viền card (`1dp` stroke, elevation `0dp`) và nhãn trạng thái chưa khớp hoàn toàn với file Figma `LTTD_QLNhaTro` (`L9hS7O7oLIp7iqyrwmr6Nq`, node `0:1`).
   - Vai trò `LANDLORD` chưa có thanh điều hướng dưới (`BottomNavigationView`), trong khi `TENANT` đã có thanh 5 tab.
   - Phát hiện thêm khi kiểm thử thực tế trên emulator:
     - `AuthViewModel` phát `_loginState` / `_registerState` trước `saveUserData()`, làm `MainActivity` đọc `role` còn trống ngay thời điểm vừa đăng nhập nên thanh BottomNav không hiện ở màn hình Home đầu tiên cho tới khi chuyển tab khác.
     - Nút `Lịch sử thanh toán` trên `fragment_tenant_invoice.xml` bị chia 2 dòng và cắt chữ ở chiều cao `46dp`.

---

## 2. Design System thống nhất theo Figma RoomCare

Hệ thống giao diện được đối chiếu trực tiếp từ Figma (`Page 1`, 39 frame tổng thể, trích xuất chi tiết 18 frame trọng tâm của luồng Auth, Landlord và Tenant):

- **Màu thương hiệu (Brand Palette):**
  - Primary Teal: `#0F766E` (`@color/rc_brand` / `@color/rc_primary`)
  - Primary Dark: `#0B5751` (`@color/rc_brand_dark`)
  - Brand Tint / Active Pill: `#DDF1EE` (`@color/rc_tint`) và Soft Tint `#E7F3F0` (`@color/rc_tint_soft`)
  - Biểu đồ doanh thu: `#DDF1EE` (`@color/chart_billed`) và `#0F766E` (`@color/chart_collected`)
- **Nền, Viền & Bề mặt (Surfaces):**
  - Canvas tổng thể: `#F6F8F7` (`@color/rc_canvas` / `@color/background_light`)
  - Bề mặt Card / Toolbar: `#FFFFFF` (`@color/surface`), bề mặt phụ `#EEF3F1` (`@color/surface_variant`)
  - Viền phân tách mảnh: `#DDE5E2` (`@color/rc_line` / `@color/divider`, dày `1dp`, không đổ bóng `cardElevation="0dp"`)
- **Typography & Màu ngữ nghĩa:**
  - Chữ chính: `#17201E` (`@color/rc_ink`), chữ phụ: `#66736F` (`@color/rc_muted`), hint: `#98A39F` (`@color/text_hint`)
  - Trạng thái: Thành công `#218A58` / nền `#E1F3E8`, Thông tin `#3E6091` / nền `#E5EDF9`, Cảnh báo `#B76B16` / nền `#FFF0D8`, Nguy hiểm/Nợ quá hạn `#C44747` / nền `#FBE8E7`, Tím accent `#7056A8` / nền `#EEE9F8`
- **Style & Shape (`roomcare_ui.xml`):**
  - `RoomCare.Card`: bo góc `16dp`, viền `1dp @color/rc_line`, elevation `0dp`.
  - `RoomCare.Button`, `RoomCare.Button.Secondary`, `RoomCare.Button.Tonal`, `RoomCare.Button.Danger`: bo góc `12dp`, chiều cao tối thiểu `48dp`.
  - `RoomCare.Input`: `OutlinedBox` bo góc `12dp`, viền `@color/rc_field_stroke`.
  - `RoomCare.BottomNav.Indicator`: pill `#DDF1EE` bo tròn đồng bộ Material 3.

---

## 3. Các thành phần & màn hình đã nâng cấp

- **Điều hướng động theo vai trò (`activity_main.xml`, `MainActivity.kt`, `landlord_navigation.xml`, `tenant_navigation.xml`):**
  - Chủ nhà (`LANDLORD`): `Trang chủ` · `Phòng` · `Hóa đơn` · `Công nợ` · `Cá nhân`.
  - Người thuê (`TENANT`): `Trang chủ` · `Phòng tôi` · `Hóa đơn` · `Thông báo` · `Cá nhân`.
  - Đồng bộ status bar sáng `#F6F8F7` trên toàn app (trừ `splashFragment` giữ nền brand `#0F766E`).
  - Sửa `AuthViewModel.kt` (`login` và `register`) để gọi `saveUserData(result.data)` trước khi phát `StateFlow`, giúp thanh BottomNav hiển thị ngay lập tức ở màn hình Home đầu tiên cho cả Chủ nhà và Người thuê.
- **Nhóm Xác thực (`fragment_splash.xml`, `fragment_login.xml`, `fragment_register.xml`, `fragment_forgot_password.xml`):**
  - Chuẩn hóa Splash brand teal với logo squircle trắng, card đăng nhập/đăng ký viền `1dp`, `RoomCare.Input` 12dp, thông điệp bảo mật ở footer.
- **Nhóm Chủ nhà (`LandlordHomeFragment.kt` + toàn bộ layout GĐ2 & GĐ4):**
  - `fragment_landlord_home.xml`: Thêm avatar chữ cái đầu (`tvAvatar`), 4 thẻ thống kê phòng (`rc_panel`), card Doanh thu & biểu đồ cột 3 tháng, 4 ô Thao tác & Quản lý nhanh.
  - Quản lý Phòng & Khách thuê (`fragment_room_list.xml`, `item_room.xml`, `fragment_room_detail.xml`, `fragment_room_form.xml`, `fragment_tenant_list.xml`, `item_tenant.xml`, `fragment_tenant_detail.xml`, `fragment_tenant_form.xml`).
  - Tài chính, Điện nước & Health Score (`fragment_invoice.xml`, `item_invoice.xml`, `fragment_invoice_detail.xml`, `fragment_debt.xml`, `item_debt.xml`, `fragment_health_score.xml`, `item_health_score.xml`, `fragment_utility.xml`, `item_utility_record.xml`), giữ nguyên `bg_badge_rounded` / `rc_icon_box_tint` để tương thích hoàn toàn với `backgroundTintList` trong các Adapter.
- **Nhóm Người thuê & Dùng chung:**
  - `fragment_tenant_home.xml`, `fragment_my_room.xml`, `fragment_my_contract.xml`, `fragment_tenant_invoice.xml` (sửa tỷ lệ weight `0.7 / 1.3` và `maxLines="1"` cho nút `Lịch sử thanh toán`), `item_tenant_invoice.xml`, `fragment_tenant_invoice_detail.xml`, `fragment_qr_payment.xml`, `fragment_payment_history.xml`, `item_tenant_payment.xml`, `fragment_incident_list.xml`, `item_incident.xml`, `fragment_incident_form.xml`, `fragment_incident_detail.xml`, `fragment_profile.xml`, `fragment_notification.xml`.
  - Giữ nguyên 100% toàn bộ `android:id` ViewBinding và không thay đổi API contract/Backend.

---

## 4. Kết quả kiểm thử & xác minh thực tế

- **Unit Test & Build (`./gradlew testDebugUnitTest installDebug`):**
  - `BUILD SUCCESSFUL` với JDK 17.
  - Tổng cộng **5/5 unit test** đạt (`ExampleUnitTest`: 1/1, `FinanceHealthScoreUnitTest`: 4/4, `skipped=0`, `failures=0`, `errors=0`).
- **Kiểm thử trực tiếp trên Android Emulator (`Medium_Phone_API_37.0` / `emulator-5554`) kết nối Backend Spring Boot (`:8080`) + MySQL (`:3306`):**
  - Đăng nhập thật bằng `landlord1@example.com` và `tenant1@example.com`: BottomNav 5 tab hiển thị ngay sau khi đăng nhập cho cả hai vai trò.
  - Kiểm tra ảnh chụp màn hình qua `adb shell screencap` trên Splash, Đăng nhập, Trang chủ Chủ nhà, Phòng, Hóa đơn Chủ nhà, Công nợ, Cá nhân, Trang chủ Người thuê, Phòng tôi, Hóa đơn Người thuê (nút `Lịch sử thanh toán` hiển thị trọn vẹn 1 dòng), Báo sự cố và Thông báo.
  - `adb logcat -d *:E` không ghi nhận `FATAL EXCEPTION` nào từ ứng dụng.
