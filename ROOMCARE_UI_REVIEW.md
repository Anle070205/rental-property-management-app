# ROOMCARE – BÁO CÁO RÀ SOÁT & NÂNG CẤP UI/UX (UI REVIEW)

## 1. Vấn đề UI/UX ban đầu sau khi merge code 3 thành viên

1. **Đứt gãy giao diện Dashboard Chủ nhà:** Do xung đột merge ở commit `49adb8f`, người dùng đăng nhập `LANDLORD` bị đưa vào `LandlordDashboardFragment` bản sơ khai chỉ có 2 nút trắng/tím, không thấy được màn hình `LandlordHomeFragment` (biểu đồ doanh thu, thống kê phòng, hóa đơn, công nợ, Health Score).
2. **Xung đột hai hệ thống màu sắc (Visual Identity Split):**
   - Phần Auth (GĐ1) và Phòng/Khách thuê (GĐ2) dùng tím Material mặc định (`#6200EE` `purple_500`, nền `#FAFAFA`, chữ đen tuyền `#000000`, nhiều card dùng `@color/design_default_color_primary`).
   - Phần Người thuê & Hồ sơ/Thông báo (GĐ5–GĐ6) dùng hệ **RoomCare Teal** hiện đại (`#10756D` `rc_brand`, nền `#F4F7F6` `rc_canvas`, chữ `#1B2927` `rc_ink`).
3. **Biểu tượng (Icons) & Ngôn ngữ thiếu đồng bộ:** Một số màn hình GĐ1–GĐ2 vẫn dùng icon mặc định cũ của Android SDK (`@android:drawable/ic_menu_revert`, `ic_menu_edit`, `ic_menu_delete`, `stat_sys_phone_call`, `ic_menu_myplaces`), nút sửa/xóa trên item phòng nhỏ (`40dp < 48dp`), hộp thoại xác nhận còn lẫn nút `"Cancel"` tiếng Anh.
4. **Thiếu điều hướng và bộ lọc trực quan:**
   - Màn hình Chủ nhà không có lối vào nhanh `Thông báo` và `Hồ sơ cá nhân`.
   - Màn hình `ProfileFragment` không có nút quay lại khi mở từ luồng Chủ nhà.
   - `RoomListFragment` chưa có dãy Chip lọc trạng thái và thanh tìm kiếm chưa hoạt động; `TenantListFragment` và `TenantDetailFragment` thiếu nút Thêm/Sửa/Xóa.

---

## 2. Design System thống nhất áp dụng cho RoomCare

Chuẩn hóa xuyên suốt giữa Chủ nhà, Người thuê và Xác thực theo phong cách **Modern SaaS / Property Management**:

- **Màu thương hiệu (Brand Palette):**
  - Primary Teal: `#10756D` (`@color/rc_brand`)
  - Primary Dark (Status Bar / Accent): `#0B5751` (`@color/rc_brand_dark`)
  - Secondary / Biểu đồ: `#26A69A` (`@color/rc_brand_light`), `#80CBC4` (`@color/chart_billed`), `#10756D` (`@color/chart_collected`)
- **Nền & Bề mặt (Surfaces):**
  - Canvas tổng thể: `#F4F7F6` (`@color/rc_canvas` / `@color/background_light`)
  - Card / Panel: `#FFFFFF` với viền mảnh `@color/rc_line` (`#E1E9E6`) và bo góc `10dp`–`12dp`
- **Typography & Màu chữ:**
  - Tiêu đề chính (`rc_ink`): `#1B2927`
  - Nội dung phụ (`rc_muted`): `#516561`
  - Ngữ nghĩa trạng thái: Xanh lá (`#4CAF50`), Xanh dương (`#2196F3`), Cam cảnh báo (`#FF9800`), Đỏ lỗi/nợ (`#F44336` / `#BC4143`)
- **Kích thước vùng bấm & Khả năng sử dụng:**
  - Nút hành động và icon button tối thiểu `44dp–48dp`, giữ định dạng tiền Việt (`1.000.000 ₫`) và ngày (`dd/MM/yyyy`).

---

## 3. Các màn hình đã nâng cấp & chuẩn hóa

- **Hệ thống Theme (`themes.xml`, `colors.xml`):** Chuyển `colorPrimary` toàn app sang `@color/rc_brand`, đồng bộ `statusBarColor`, màu nền `background_light`, màu chữ `text_primary`/`text_secondary` và màu biểu đồ doanh thu.
- **`LoginFragment` (`fragment_login.xml`):** Đồng bộ nền `rc_canvas`, ô nhập `@style/RoomCare.Input`, nút `@style/RoomCare.Button`, thay vector icon thống nhất (`ic_email`, `ic_info`), tăng vùng chạm `Quên mật khẩu?` lên tối thiểu `48dp`.
- **`RegisterFragment` (`fragment_register.xml`, `RegisterFragment.kt`):** Thiết kế lại form đăng ký đồng bộ `RoomCare.Input` & `RoomCare.Button`, bổ sung nhóm chọn **Loại tài khoản (`Chủ nhà` / `Người thuê`)**, thay thế toàn bộ icon SDK cũ.
- **`LandlordHomeFragment` (`fragment_landlord_home.xml`, `LandlordHomeFragment.kt`):** Khôi phục làm màn hình Dashboard chính cho Chủ nhà với các thẻ tóm tắt phòng, tài chính, biểu đồ doanh thu theo tháng, hợp đồng sắp hết hạn, phòng cần chú ý; bổ sung nút `Thông báo` & `Hồ sơ` ngay trên header cùng hộp thoại xác nhận đăng xuất tiếng Việt.
- **`RoomListFragment` & `item_room.xml`:** Thay toolbar có nút Quay lại, nâng cấp thanh tìm kiếm theo mã phòng/nhà trọ/người thuê, bổ sung hàng `ChipGroup` lọc nhanh theo trạng thái (`Tất cả`, `Trống`, `Đã thuê`, `Sắp hết hạn`, `Cần chú ý`), tăng kích thước nút `Sửa`/`Xóa` lên `48dp` với vector icon `ic_edit`/`ic_delete`.
- **`RoomDetailFragment` (`fragment_room_detail.xml`):** Đồng bộ icon quay lại `ic_arrow_back` trên AppBar và nối điều hướng `navigateUp()`.
- **`TenantListFragment`, `TenantDetailFragment`, `item_tenant.xml`:** Đồng bộ màu chữ/icon `rc_brand` & `ic_phone`, bổ sung `FloatingActionButton` thêm người thuê, hoàn thiện các nút `Chỉnh sửa thông tin` (`RoomCare.Button`), `Xóa người thuê` (`RoomCare.Button.Danger` có xác nhận) và `Quay lại` (`RoomCare.Button.Secondary`).
- **`ProfileFragment` & `NotificationFragment`:** Bổ sung `MaterialToolbar` có nút quay lại cho Chủ nhà ở màn hình Cá nhân, đồng bộ thông báo tiếng Việt (`"Hủy"` thay cho `"Cancel"`), giữ nguyên trải nghiệm Bottom Navigation mượt mà cho Người thuê.

---

## 4. Thành phần UI được tái sử dụng & Khác biệt có chủ đích

- **Tái sử dụng:** Giữ nguyên các style chung trong `roomcare_ui.xml` (`RoomCare.Button`, `RoomCare.Button.Secondary`, `RoomCare.Button.Danger`, `RoomCare.Input`, `RoomCare.Toolbar`), dùng chung `ProfileFragment` & `NotificationFragment` cho cả Chủ nhà và Người thuê, dùng chung `RoomFormFragment` và `TenantFormFragment` cho thêm mới và chỉnh sửa.
- **Khác biệt & Giới hạn đối chiếu Figma:** Do không thể truy cập trực tiếp liên kết Figma từ môi trường kiểm thử tự động, nhóm đã đối chiếu gián tiếp qua các màn hình mẫu chuẩn của thành viên 3 (`roomcare_ui.xml`, `TenantHomeFragment`, `NotificationFragment`) đã dựng theo Figma và nhân rộng hệ thống thiết kế này cho toàn bộ ứng dụng.
- **Kiểm chứng trên Emulator (`Medium_Phone_API_37.0`):** Toàn bộ các màn hình sau nâng cấp đã được build và chạy thực tế qua `uiautomator dump`, không bị tràn chữ, không che nút và không có `FATAL EXCEPTION` trong `adb logcat`.
