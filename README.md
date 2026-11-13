# RoomCare - Ứng dụng Quản lý Nhà trọ

Ứng dụng Android kết hợp Backend Spring Boot để quản lý nhà trọ, phòng, hợp đồng và người thuê.

## 🎯 Giai đoạn 1 - Hoàn thành ✅

### Tính năng
- ✅ Đăng ký tài khoản Chủ nhà (LANDLORD)
- ✅ Đăng nhập (email/phone + password)
- ✅ Auto-login với JWT token
- ✅ Phân biệt role LANDLORD/TENANT
- ✅ Validation đầy đủ
- ✅ Password encryption (BCrypt)
- ✅ Error handling

## 🛠️ Tech Stack

### Android Frontend
- **Language:** Kotlin
- **UI:** XML Layout + View Binding
- **Architecture:** MVVM
- **Navigation:** Navigation Component
- **Network:** Retrofit + OkHttp + Gson
- **Async:** Kotlin Coroutines + StateFlow
- **DI:** Manual (Repository pattern)

### Backend
- **Framework:** Spring Boot 3.2.0
- **Language:** Java 17
- **Build Tool:** Maven
- **Database:** MySQL 8
- **Security:** Spring Security + JWT
- **Password:** BCrypt
- **ORM:** Hibernate (Spring Data JPA)

## 📦 Cấu trúc Project

```
rental-property-management-app/
├── backend/          Spring Boot Backend
└── frontend/         Android App
```

## 🚀 Cách chạy

### Backend
```bash
cd backend
mvn spring-boot:run
```
Backend chạy trên: `http://localhost:8080`

### Android
1. Mở `frontend` trong Android Studio
2. Start Android Emulator
3. Run app
4. App sẽ kết nối Backend qua `http://192.168.100.231:8080`

## 🔑 API Endpoints

### Authentication
- `POST /api/auth/register` - Đăng ký LANDLORD
- `POST /api/auth/login` - Đăng nhập
- `POST /api/auth/logout` - Đăng xuất
- `POST /api/auth/forgot-password` - Quên mật khẩu

### Test
- `GET /api/test` - Health check

## 🗄️ Database

**Database:** `roomcare`

**Tables:**
- `users` - Thông tin user (id, fullName, email, phone, password, role, isActive, createdAt, updatedAt)

## 👥 Team Members

- **Member 1:** Platform foundation, Authentication, Property & Room Management
- **Member 2:** [Phân công sau]
- **Member 3:** [Phân công sau]

## 📝 Business Rules

1. **LANDLORD** tự đăng ký tài khoản
2. **TENANT** được tạo bởi LANDLORD khi tạo hợp đồng
3. Password phải ít nhất 6 ký tự
4. Email và Phone phải unique
5. JWT token expires sau 24 giờ

## 🔐 Security

- Password được hash bằng BCrypt (KHÔNG lưu plain text)
- JWT stateless authentication
- Role-based access control
- Network Security Config cho HTTP (dev only)

## 📚 Next Steps

- [ ] Giai đoạn 2: Property & Room Management
- [ ] Giai đoạn 3: Contract & Tenant Management
- [ ] Giai đoạn 4: Invoice & Payment
- [ ] Giai đoạn 5: Notification & Dashboard

## 📄 License

Educational project for University

---

**Last Updated:** November 13, 2026
**Status:** Phase 1 Completed ✅
