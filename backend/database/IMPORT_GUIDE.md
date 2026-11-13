# 🚀 Quick Import Guide

## 1️⃣ Import Database (Chọn 1 cách)

### Cách 1: Command Line ⚡ (Nhanh nhất)
```bash
mysql -u root -p < roomcare_phase2.sql
```

### Cách 2: MySQL Workbench 🖱️
File > Run SQL Script > Chọn `roomcare_phase2.sql` > Run

### Cách 3: phpMyAdmin 🌐
Import tab > Choose File > `roomcare_phase2.sql` > Go

---

## 2️⃣ Test Login

**Accounts** (Password: `123456`):
- `landlord1@example.com` / `0901234567`
- `landlord2@example.com` / `0912345678`
- `tenant1@example.com` / `0923456789`

**Login API**:
```bash
POST http://localhost:8080/api/auth/login
{
  "emailOrPhone": "landlord1@example.com",
  "password": "123456"
}
```

---

## 3️⃣ Verify

```sql
USE RoomCare;
SELECT 'Users', COUNT(*) FROM users
UNION ALL SELECT 'Properties', COUNT(*) FROM properties
UNION ALL SELECT 'Rooms', COUNT(*) FROM rooms
UNION ALL SELECT 'Tenants', COUNT(*) FROM tenants;
```

Kết quả: 3 users, 3 properties, 6 rooms, 3 tenants ✅

---

## 📖 More Details

- **Full README**: `README.md` trong thư mục này
- **API Documentation**: `../PHASE2_README.md`
- **Postman Collection**: `../RoomCare_Phase2_Postman.json`

---

**That's it!** Database ready to use! 🎉
