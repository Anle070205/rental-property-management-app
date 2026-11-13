# GIAI ĐOẠN 2 - BACKEND: Property, Room & Tenant Management

## ✅ Hoàn thành 100%

Phase 2 Backend đã được triển khai đầy đủ với quản lý nhà trọ, phòng trọ và khách thuê cho Chủ nhà (Landlord).

---

## 📋 Tổng quan

Phase 2 bổ sung các entity và API để Chủ nhà quản lý:
- **Properties (Nhà trọ)**: CRUD operations
- **Rooms (Phòng trọ)**: CRUD với filters (trạng thái, giá, tầng)
- **Tenants (Khách thuê)**: CRUD với search (tên, số điện thoại)

---

## 🗂️ Cấu trúc Project

```
backend/
├── src/main/java/com/roomcare/backend/
│   ├── config/
│   │   ├── SecurityConfig.java (✅ Updated - Role-based security)
│   │   └── DataSeeder.java (✅ NEW - Seed data cho dev)
│   ├── controller/
│   │   ├── PropertyController.java (✅ NEW)
│   │   ├── RoomController.java (✅ NEW)
│   │   └── TenantController.java (✅ NEW)
│   ├── dto/
│   │   ├── PageResponse.java (✅ NEW - Pagination response)
│   │   ├── property/
│   │   │   ├── CreatePropertyRequest.java
│   │   │   ├── UpdatePropertyRequest.java
│   │   │   └── PropertyResponse.java
│   │   ├── room/
│   │   │   ├── CreateRoomRequest.java
│   │   │   ├── UpdateRoomRequest.java
│   │   │   └── RoomResponse.java
│   │   └── tenant/
│   │       ├── CreateTenantRequest.java
│   │       ├── UpdateTenantRequest.java
│   │       └── TenantResponse.java
│   ├── entity/
│   │   ├── Property.java (✅ NEW)
│   │   ├── Room.java (✅ NEW)
│   │   ├── Tenant.java (✅ NEW)
│   │   └── RoomStatus.java (✅ NEW - enum)
│   ├── exception/
│   │   └── GlobalExceptionHandler.java (✅ NEW - Unified error handling)
│   ├── repository/
│   │   ├── PropertyRepository.java (✅ NEW)
│   │   ├── RoomRepository.java (✅ NEW)
│   │   └── TenantRepository.java (✅ NEW)
│   ├── security/
│   │   └── JwtAuthenticationFilter.java (✅ NEW)
│   └── service/
│       ├── PropertyService.java (✅ NEW)
│       ├── RoomService.java (✅ NEW)
│       └── TenantService.java (✅ NEW)
```

---

## 📊 Database Schema

### 1. Properties Table
```sql
CREATE TABLE properties (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    landlord_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL,
    description TEXT,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (landlord_id) REFERENCES users(id)
);
```

### 2. Rooms Table
```sql
CREATE TABLE rooms (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    property_id BIGINT NOT NULL,
    room_code VARCHAR(50) NOT NULL,
    floor INT NOT NULL,
    area DECIMAL(10,2) NOT NULL,
    rent_price DECIMAL(15,2) NOT NULL,
    deposit DECIMAL(15,2) NOT NULL,
    capacity INT NOT NULL,
    amenities TEXT,
    description TEXT,
    image_url VARCHAR(255),
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (property_id) REFERENCES properties(id),
    UNIQUE KEY unique_room_code (property_id, room_code)
);
```

### 3. Tenants Table
```sql
CREATE TABLE tenants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT,
    landlord_id BIGINT NOT NULL,
    property_id BIGINT NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(100),
    identity_number VARCHAR(20) NOT NULL UNIQUE,
    date_of_birth DATE,
    address TEXT,
    emergency_contact VARCHAR(100),
    created_at DATETIME NOT NULL,
    updated_at DATETIME NOT NULL,
    FOREIGN KEY (user_id) REFERENCES users(id),
    FOREIGN KEY (landlord_id) REFERENCES users(id),
    FOREIGN KEY (property_id) REFERENCES properties(id)
);
```

---

## 🔐 Security & Authorization

### Role-Based Access Control
- **ALL** `/api/landlord/**` endpoints require `LANDLORD` role
- JWT token must be included: `Authorization: Bearer <token>`
- Each landlord can only access their own data (ownership validation in services)

### Ownership Checks
```java
// Tất cả services đều kiểm tra quyền sở hữu:
if (!property.getLandlord().getId().equals(landlordId)) {
    throw new RuntimeException("Bạn không có quyền truy cập");
}
```

---

## 🚀 API Endpoints

### Property APIs (`/api/landlord/properties`)

#### 1. Tạo nhà trọ mới
```http
POST /api/landlord/properties
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Nhà trọ Hoàng Anh",
  "address": "123 Nguyễn Trãi, Quận 5, TP.HCM",
  "description": "Nhà trọ cao cấp, gần trường đại học"
}
```

#### 2. Lấy danh sách nhà trọ (phân trang)
```http
GET /api/landlord/properties?page=0&size=10&sortBy=createdAt&sortDir=desc
Authorization: Bearer <token>
```

Response:
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": 1,
        "landlordId": 1,
        "landlordName": "Nguyễn Văn A",
        "name": "Nhà trọ Hoàng Anh",
        "address": "123 Nguyễn Trãi, Quận 5, TP.HCM",
        "description": "Nhà trọ cao cấp",
        "totalRooms": 3,
        "vacantRooms": 1,
        "occupiedRooms": 2,
        "createdAt": "2024-11-14T03:00:00",
        "updatedAt": "2024-11-14T03:00:00"
      }
    ],
    "page": 0,
    "size": 10,
    "totalElements": 1,
    "totalPages": 1
  }
}
```

#### 3. Lấy chi tiết nhà trọ
```http
GET /api/landlord/properties/{id}
Authorization: Bearer <token>
```

#### 4. Cập nhật nhà trọ
```http
PUT /api/landlord/properties/{id}
Authorization: Bearer <token>
Content-Type: application/json

{
  "name": "Nhà trọ Hoàng Anh (Updated)",
  "address": "123 Nguyễn Trãi, Quận 5, TP.HCM",
  "description": "Mô tả mới"
}
```

---

### Room APIs (`/api/landlord/rooms`)

#### 1. Tạo phòng mới
```http
POST /api/landlord/rooms
Authorization: Bearer <token>
Content-Type: application/json

{
  "propertyId": 1,
  "roomCode": "101",
  "floor": 1,
  "area": 20.00,
  "rentPrice": 2500000,
  "deposit": 2500000,
  "capacity": 2,
  "amenities": "Điều hòa, Nóng lạnh, Giường, Tủ",
  "description": "Phòng đẹp, thoáng mát",
  "imageUrl": null
}
```

#### 2. Lấy tất cả phòng của landlord
```http
GET /api/landlord/rooms?page=0&size=10&sortBy=createdAt&sortDir=desc
Authorization: Bearer <token>
```

#### 3. Lấy phòng theo property (có filters)
```http
GET /api/landlord/rooms/property/{propertyId}?status=VACANT&minPrice=2000000&maxPrice=3000000&floor=1&page=0&size=10
Authorization: Bearer <token>
```

Query Parameters:
- `status`: VACANT | OCCUPIED | EXPIRING_SOON | NEEDS_ATTENTION (optional)
- `minPrice`: Giá tối thiểu (optional)
- `maxPrice`: Giá tối đa (optional)
- `floor`: Tầng (optional)
- `page`, `size`, `sortBy`, `sortDir`: Pagination params

#### 4. Lấy chi tiết phòng
```http
GET /api/landlord/rooms/{id}
Authorization: Bearer <token>
```

#### 5. Cập nhật phòng
```http
PUT /api/landlord/rooms/{id}
Authorization: Bearer <token>
Content-Type: application/json

{
  "roomCode": "101",
  "floor": 1,
  "area": 22.00,
  "rentPrice": 2700000,
  "deposit": 2700000,
  "capacity": 2,
  "amenities": "Điều hòa, Nóng lạnh, Giường, Tủ, Ban công",
  "description": "Updated description",
  "imageUrl": null,
  "status": "OCCUPIED"
}
```

#### 6. Xóa phòng
```http
DELETE /api/landlord/rooms/{id}
Authorization: Bearer <token>
```

---

### Tenant APIs (`/api/landlord/tenants`)

#### 1. Tạo khách thuê mới
```http
POST /api/landlord/tenants
Authorization: Bearer <token>
Content-Type: application/json

{
  "propertyId": 1,
  "fullName": "Lê Văn C",
  "phone": "0923456789",
  "email": "levane@example.com",
  "identityNumber": "079012345678",
  "dateOfBirth": "1995-05-15",
  "address": "Bình Dương",
  "emergencyContact": "0987654321"
}
```

#### 2. Lấy danh sách khách thuê (có filters)
```http
GET /api/landlord/tenants?propertyId=1&keyword=Lê&page=0&size=10&sortBy=createdAt&sortDir=desc
Authorization: Bearer <token>
```

Query Parameters:
- `propertyId`: Lọc theo nhà trọ (optional)
- `keyword`: Tìm kiếm theo tên hoặc số điện thoại (optional)
- `page`, `size`, `sortBy`, `sortDir`: Pagination params

Response (List view - CMND được mask):
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "content": [
      {
        "id": 1,
        "fullName": "Lê Văn C",
        "phone": "0923456789",
        "email": "levane@example.com",
        "identityNumber": "********5678",
        "propertyName": "Nhà trọ Hoàng Anh",
        ...
      }
    ]
  }
}
```

#### 3. Lấy chi tiết khách thuê
```http
GET /api/landlord/tenants/{id}
Authorization: Bearer <token>
```

Response (Detail view - CMND đầy đủ):
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "id": 1,
    "fullName": "Lê Văn C",
    "phone": "0923456789",
    "email": "levane@example.com",
    "identityNumber": "079012345678",
    ...
  }
}
```

#### 4. Cập nhật khách thuê
```http
PUT /api/landlord/tenants/{id}
Authorization: Bearer <token>
Content-Type: application/json

{
  "fullName": "Lê Văn C (Updated)",
  "phone": "0923456789",
  "email": "levanc.new@example.com",
  "identityNumber": "079012345678",
  "dateOfBirth": "1995-05-15",
  "address": "Địa chỉ mới",
  "emergencyContact": "0987654321"
}
```

#### 5. Xóa khách thuê
```http
DELETE /api/landlord/tenants/{id}
Authorization: Bearer <token>
```

---

## ✅ Business Rules

### Property
- ✅ Mỗi property thuộc về 1 landlord
- ✅ Landlord chỉ thấy/sửa property của mình
- ✅ Property response bao gồm thống kê phòng (total, vacant, occupied)

### Room
- ✅ Mỗi room thuộc về 1 property
- ✅ `room_code` phải unique trong cùng 1 property (constraint UNIQUE)
- ✅ `rentPrice` > 0, `deposit` >= 0 (validation)
- ✅ `capacity` >= 1 (validation)
- ✅ Trạng thái: VACANT | OCCUPIED | EXPIRING_SOON | NEEDS_ATTENTION
- ✅ Mặc định status = VACANT khi tạo mới
- ✅ Giá tiền dùng BigDecimal/DECIMAL(15,2)
- ✅ Diện tích dùng DECIMAL(10,2)
- ⏳ TODO Phase 3: Kiểm tra hợp đồng trước khi xóa phòng

### Tenant
- ✅ Mỗi tenant thuộc về 1 landlord và 1 property
- ✅ `user_id` nullable (khách thuê có thể chưa có tài khoản)
- ✅ `phone` và `identityNumber` phải unique
- ✅ CMND/CCCD được mask (****xxxx) trong list view, hiện đầy đủ ở detail view
- ✅ Validation: phone (10-11 số), identityNumber (9-12 số), email format
- ⏳ TODO Phase 3: Kiểm tra hợp đồng trước khi xóa tenant

---

## 🔄 Pagination Response Format

Tất cả danh sách đều trả về theo format:
```json
{
  "success": true,
  "message": "Success",
  "data": {
    "content": [...],
    "page": 0,
    "size": 10,
    "totalElements": 25,
    "totalPages": 3
  }
}
```

Default pagination:
- `page`: 0 (first page)
- `size`: 10
- `sortBy`: "createdAt"
- `sortDir`: "desc"

---

## 🔍 Validation

### Property
- `name`: required, max 100 chars
- `address`: required, max 255 chars
- `description`: optional, max 1000 chars

### Room
- `propertyId`: required
- `roomCode`: required, max 50 chars
- `floor`: required, >= 0
- `area`: required, > 0
- `rentPrice`: required, > 0
- `deposit`: required, >= 0
- `capacity`: required, >= 1
- `amenities`, `description`, `imageUrl`: optional

### Tenant
- `propertyId`: required
- `fullName`: required, max 100 chars
- `phone`: required, regex ^[0-9]{10,11}$
- `email`: optional, valid email format
- `identityNumber`: required, regex ^[0-9]{9,12}$
- `dateOfBirth`: optional, must be past date
- `address`: optional, max 255 chars
- `emergencyContact`: optional, max 100 chars

---

## 🗄️ Database Setup

### Option 1: Import SQL File (Recommended cho Team)

File SQL đã chuẩn bị sẵn: `backend/database/roomcare_phase2.sql`

```bash
# Import database
cd backend/database
mysql -u root -p < roomcare_phase2.sql

# Hoặc với password trực tiếp
mysql -u root -p@An07022005 < roomcare_phase2.sql
```

**Hoặc dùng MySQL Workbench**: File > Run SQL Script > Chọn `roomcare_phase2.sql`

### Option 2: DataSeeder (Auto)

Nếu không import SQL, DataSeeder sẽ tự động chạy khi:
- `spring.profiles.active=dev` (đã config sẵn)
- Database rỗng (chưa có users)

```bash
cd backend
./mvnw spring-boot:run
# hoặc
mvn spring-boot:run
```

### Test Accounts (Password: `123456`)

### Test Accounts (Password: `123456`)

**Landlord Accounts:**
- Email: `landlord1@example.com`, Phone: `0901234567`
- Email: `landlord2@example.com`, Phone: `0912345678`

**Tenant Account:**
- Email: `tenant1@example.com`, Phone: `0923456789`

### Sample Data

**Properties:**
- Nhà trọ Hoàng Anh (landlord1) - 3 phòng
- Nhà trọ Minh Khang (landlord1) - 2 phòng
- Nhà trọ Bình An (landlord2) - 1 phòng

**Rooms:** 6 phòng với các trạng thái khác nhau
**Tenants:** 3 khách thuê

**Chi tiết đầy đủ**: Xem `backend/database/README.md`

---

### 4. Test workflow:

#### Step 1: Login
```bash
POST http://localhost:8080/api/auth/login
{
  "emailOrPhone": "landlord1@example.com",
  "password": "123456"
}
```

Copy `token` từ response.

#### Step 2: Get properties
```bash
GET http://localhost:8080/api/landlord/properties
Authorization: Bearer <token>
```

#### Step 3: Get rooms by property
```bash
GET http://localhost:8080/api/landlord/rooms/property/1?status=VACANT
Authorization: Bearer <token>
```

#### Step 4: Get tenants
```bash
GET http://localhost:8080/api/landlord/tenants?keyword=Lê
Authorization: Bearer <token>
```

---

## 🛡️ Error Handling

### GlobalExceptionHandler
- **RuntimeException**: 400 Bad Request với message
- **MethodArgumentNotValidException**: 400 Bad Request với field errors
- **Generic Exception**: 500 Internal Server Error

### Error Response Format
```json
{
  "success": false,
  "message": "Bạn không có quyền truy cập nhà trọ này",
  "data": null
}
```

Validation Errors:
```json
{
  "success": false,
  "message": "Dữ liệu không hợp lệ",
  "data": {
    "name": "Tên nhà trọ không được để trống",
    "address": "Địa chỉ không được để trống"
  }
}
```

---

## ⏳ TODO - Phase 3 & 4

### Phase 3 - Contract Management
- [ ] Contract entity (hợp đồng thuê phòng)
- [ ] Kiểm tra active contract trước khi xóa Room/Tenant
- [ ] Tự động cập nhật Room status dựa trên Contract
- [ ] Populate `currentContract`, `currentTenant`, `currentRoom` trong responses

### Phase 4 - Tenant Features & Bills
- [ ] Tenant APIs (`/api/tenant/**`)
- [ ] Bill/Invoice entity và APIs
- [ ] Payment tracking
- [ ] Notifications

---

## 📝 Notes

1. **Database Migration**: Schema tự động update qua `spring.jpa.hibernate.ddl-auto=update`
2. **Seed Data**: Chỉ chạy 1 lần khi database trống (dev profile)
3. **JWT Secret**: Đổi `jwt.secret` trong production
4. **Password**: Tất cả test accounts dùng password `123456` (BCrypt hashed)
5. **Timezone**: Đã config `Asia/Ho_Chi_Minh` cho Jackson và Database
6. **Role Format**: Spring Security yêu cầu prefix `ROLE_`, đã handle trong JwtAuthenticationFilter

---

## ✅ Checklist Hoàn thành

- [x] Entity: Property, Room, Tenant, RoomStatus enum
- [x] Repository: PropertyRepository, RoomRepository, TenantRepository với custom queries
- [x] DTO: Request/Response cho tất cả entities, PageResponse
- [x] Service: PropertyService, RoomService, TenantService với ownership validation
- [x] Controller: PropertyController, RoomController, TenantController
- [x] Security: Role-based access control cho /api/landlord/**
- [x] JwtAuthenticationFilter: JWT token processing
- [x] GlobalExceptionHandler: Unified error handling
- [x] Validation: Bean validation cho tất cả requests
- [x] Pagination: Tất cả list endpoints
- [x] Filters: Room (status, price, floor), Tenant (property, keyword)
- [x] Business rules: Unique constraints, price validation, identity masking
- [x] Seed data: DataSeeder với @Profile("dev")
- [x] Documentation: API docs, business rules, testing guide

---

## 🎉 Kết quả

**Phase 2 Backend hoàn thành 100%** với đầy đủ chức năng quản lý Property, Room, Tenant cho Landlord. Tất cả APIs đã được thiết kế theo đúng convention của Phase 1, với phân quyền chặt chẽ, validation đầy đủ, và error handling thống nhất.

**Ready for Phase 3!** 🚀
