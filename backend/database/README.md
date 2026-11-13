# RoomCare Database Setup

## 📋 Tổng quan

File `roomcare_phase2.sql` chứa:
- ✅ Schema đầy đủ (4 tables: users, properties, rooms, tenants)
- ✅ Sample data (3 users, 3 properties, 6 rooms, 3 tenants)
- ✅ Indexes và Foreign Keys
- ✅ Constraints (UNIQUE, NOT NULL)

---

## 🚀 Cách import Database

### Option 1: Command Line (Recommended)
```bash
# Từ thư mục backend/database
mysql -u root -p < roomcare_phase2.sql

# Hoặc với password trong command
mysql -u root -p@An07022005 < roomcare_phase2.sql
```

### Option 2: MySQL Workbench
1. Mở MySQL Workbench
2. Connect tới MySQL Server
3. File > Run SQL Script...
4. Chọn file `roomcare_phase2.sql`
5. Click "Run"

### Option 3: phpMyAdmin
1. Mở phpMyAdmin
2. Click tab "Import"
3. Choose File: `roomcare_phase2.sql`
4. Click "Go"

---

## 👥 Test Accounts (Password: `123456`)

### Landlord Accounts
```
Email: landlord1@example.com
Phone: 0901234567
Role: LANDLORD
Properties: Nhà trọ Hoàng Anh, Nhà trọ Minh Khang

Email: landlord2@example.com
Phone: 0912345678
Role: LANDLORD
Properties: Nhà trọ Bình An
```

### Tenant Account
```
Email: tenant1@example.com
Phone: 0923456789
Role: TENANT
```

---

## 📊 Sample Data

### Properties: 3
1. **Nhà trọ Hoàng Anh** (Landlord 1) - 3 phòng
   - 101: VACANT (2.5M/tháng)
   - 102: OCCUPIED (3.0M/tháng)
   - 201: EXPIRING_SOON (2.7M/tháng)

2. **Nhà trọ Minh Khang** (Landlord 1) - 2 phòng
   - 1A: VACANT (2.0M/tháng)
   - 2B: NEEDS_ATTENTION (2.3M/tháng)

3. **Nhà trọ Bình An** (Landlord 2) - 1 phòng
   - A1: VACANT (4.0M/tháng)

### Tenants: 3
- Lê Văn C (có tài khoản user)
- Phạm Thị D (chưa có tài khoản)
- Hoàng Văn E (chưa có tài khoản)

---

## 🔐 Password Hash

Password `123456` được hash bằng BCrypt:
```
$2a$10$N9qo8uLOickgx2ZMRZoMye1pHPWKjLLsCJgFBN9xKcCflGJRJq.8q
```

⚠️ **LƯU Ý**: Nếu login không thành công, có thể hash không khớp với version BCrypt của bạn.

**Giải pháp**:
1. Register account mới qua API
2. Hoặc chạy backend với DataSeeder (sẽ tự động tạo hash đúng)

---

## ✅ Verify Import Success

Chạy query sau để kiểm tra:
```sql
USE RoomCare;

-- Kiểm tra số lượng records
SELECT 'Users' AS Table_Name, COUNT(*) AS Record_Count FROM users
UNION ALL
SELECT 'Properties', COUNT(*) FROM properties
UNION ALL
SELECT 'Rooms', COUNT(*) FROM rooms
UNION ALL
SELECT 'Tenants', COUNT(*) FROM tenants;
```

**Kết quả mong đợi**:
```
Users: 3
Properties: 3
Rooms: 6
Tenants: 3
```

---

## 🔄 Reset Database (Nếu cần)

```sql
DROP DATABASE IF EXISTS RoomCare;
```

Sau đó import lại file SQL.

---

## 🧪 Test Queries

### 1. Xem properties của landlord 1
```sql
SELECT p.*, 
       (SELECT COUNT(*) FROM rooms WHERE property_id = p.id) AS total_rooms,
       (SELECT COUNT(*) FROM rooms WHERE property_id = p.id AND status = 'VACANT') AS vacant_rooms
FROM properties p 
WHERE p.landlord_id = 1;
```

### 2. Xem rooms của property 1
```sql
SELECT * FROM rooms WHERE property_id = 1 ORDER BY room_code;
```

### 3. Xem tenants của landlord 1
```sql
SELECT t.*, p.name AS property_name
FROM tenants t
JOIN properties p ON t.property_id = p.id
WHERE t.landlord_id = 1;
```

### 4. Xem phòng trống
```sql
SELECT r.*, p.name AS property_name, p.address
FROM rooms r
JOIN properties p ON r.property_id = p.id
WHERE r.status = 'VACANT'
ORDER BY r.rent_price;
```

---

## 📝 Database Schema

### users
- id, full_name, email, phone, password, role, is_active, created_at, updated_at

### properties
- id, landlord_id (FK→users), name, address, description, created_at, updated_at

### rooms
- id, property_id (FK→properties), room_code, floor, area, rent_price, deposit, capacity, amenities, description, image_url, status, created_at, updated_at
- **UNIQUE**: (property_id, room_code)

### tenants
- id, user_id (FK→users, nullable), landlord_id (FK→users), property_id (FK→properties), full_name, phone, email, identity_number, date_of_birth, address, emergency_contact, created_at, updated_at
- **UNIQUE**: phone, identity_number

---

## 🎯 Next Steps

1. ✅ Import database: `mysql -u root -p < roomcare_phase2.sql`
2. ✅ Verify import: Check record counts
3. ✅ Update `application.properties` với database credentials
4. ✅ Start backend: `mvn spring-boot:run`
5. ✅ Test login với accounts trên
6. ✅ Test APIs với Postman collection

---

## 💡 Tips

- **Commit file SQL vào Git** để team cùng dùng
- **Không commit password thật** vào Git
- **Update credentials** trong `application.properties` theo local setup
- **Dùng .gitignore** cho `application-local.properties`

---

## 🐛 Troubleshooting

### Login failed với password "123456"
➡️ BCrypt hash không khớp. Register account mới qua API.

### Foreign Key constraint error khi import
➡️ Chạy lại từ đầu: DROP DATABASE → import lại.

### Database already exists
➡️ File SQL đã có `DROP DATABASE IF EXISTS`, chỉ cần import lại.

### Connection refused
➡️ Kiểm tra MySQL server đã chạy chưa: `mysql -u root -p`

---

**Ready to use!** 🚀
