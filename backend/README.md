# RoomCare Backend API

Backend REST API cho ứng dụng quản lý nhà trọ RoomCare.

## Tech Stack

- **Java**: 21
- **Spring Boot**: 3.2.0
- **Spring Security**: JWT Authentication
- **Spring Data JPA**: ORM with Hibernate
- **MySQL**: 8.0
- **Maven**: Build tool

## Prerequisites

- JDK 21 hoặc cao hơn
- MySQL 8.0 hoặc cao hơn
- Maven 3.6+ (hoặc sử dụng mvnw wrapper)

## Database Setup

1. Cài đặt MySQL
2. Tạo database (tự động nếu chưa có):
```sql
CREATE DATABASE roomcare;
```

3. Cập nhật thông tin database trong `application.properties`:
```properties
spring.datasource.username=root
spring.datasource.password=your_password
```

## Cách chạy

### Option 1: Sử dụng Maven
```bash
cd backend
mvn spring-boot:run
```

### Option 2: Sử dụng Maven Wrapper (Windows)
```bash
cd backend
.\mvnw.cmd spring-boot:run
```

### Option 3: Build và chạy JAR
```bash
mvn clean package
java -jar target/backend-1.0.0.jar
```

## Test API

Backend chạy trên: `http://localhost:8080`

### Test Endpoint
```bash
GET http://localhost:8080/api/test
```

Expected Response:
```json
{
  "status": "success",
  "message": "RoomCare Backend is running successfully!",
  "timestamp": "2024-11-13T10:30:00",
  "version": "1.0.0"
}
```

### Health Check
```bash
GET http://localhost:8080/api/test/health
```

## Package Structure

```
com.roomcare.backend/
├── config/          # Configuration classes (Security, CORS, etc.)
├── security/        # JWT, Authentication components
├── auth/            # Authentication endpoints
├── user/            # User management
├── controller/      # REST Controllers
├── service/         # Business Logic
├── repository/      # Data Access Layer
├── entity/          # JPA Entities
└── dto/             # Data Transfer Objects
```

## API Endpoints (Planned)

### Authentication
- POST `/api/auth/register` - Đăng ký
- POST `/api/auth/login` - Đăng nhập
- POST `/api/auth/logout` - Đăng xuất
- POST `/api/auth/refresh-token` - Refresh token
- POST `/api/auth/forgot-password` - Quên mật khẩu

### Test
- GET `/api/test` - Test API
- GET `/api/test/health` - Health check

## Development Notes

- **Port**: 8080
- **Database**: roomcare (auto-created)
- **JWT Secret**: Configured in application.properties
- **CORS**: Enabled for all origins (development only)
- **Security**: Temporarily disabled for testing (will be enabled with JWT)

## Next Steps

1. ✅ Setup project structure
2. ✅ Configure database
3. ✅ Create test endpoint
4. ⏳ Implement User entity
5. ⏳ Implement JWT authentication
6. ⏳ Implement Registration & Login APIs
7. ⏳ Implement role-based authorization

## Team Members

- Member 1: Platform foundation, Authentication, Room & Property Management
- Member 2: [To be defined]
- Member 3: [To be defined]
