-- =====================================================
-- RoomCare Database - Phase 2
-- Quản lý Nhà trọ, Phòng trọ, Khách thuê
-- =====================================================

-- Drop database nếu tồn tại và tạo mới
DROP DATABASE IF EXISTS RoomCare;
CREATE DATABASE RoomCare CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE RoomCare;

-- =====================================================
-- 1. USERS TABLE (Phase 1 + Phase 2)
-- =====================================================
CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(15) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    email_verified BOOLEAN NOT NULL DEFAULT TRUE,
    email_verified_at DATETIME NULL,
    token_version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_phone (phone),
    INDEX idx_role (role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 2. PROPERTIES TABLE (Phase 2)
-- =====================================================
CREATE TABLE properties (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    landlord_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    address VARCHAR(255) NOT NULL,
    description TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (landlord_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_landlord (landlord_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 3. ROOMS TABLE (Phase 2)
-- =====================================================
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
    status VARCHAR(20) NOT NULL DEFAULT 'VACANT',
    health_score INT NOT NULL DEFAULT 100,
    electricity_price DECIMAL(15,2),
    water_price DECIMAL(15,2),
    internet_fee DECIMAL(15,2),
    service_fee DECIMAL(15,2),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE,
    UNIQUE KEY unique_room_code (property_id, room_code),
    INDEX idx_property (property_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 4. TENANTS TABLE (Phase 2)
-- =====================================================
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
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    FOREIGN KEY (landlord_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE,
    INDEX idx_landlord (landlord_id),
    INDEX idx_property (property_id),
    INDEX idx_phone (phone),
    INDEX idx_identity (identity_number)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 5. BOOKING_REQUESTS TABLE (Đặt phòng & Liên hệ)
-- =====================================================
CREATE TABLE booking_requests (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    room_id BIGINT NOT NULL,
    property_id BIGINT NOT NULL,
    landlord_id BIGINT NOT NULL,
    tenant_user_id BIGINT NOT NULL,
    request_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    tenant_name VARCHAR(100) NOT NULL,
    tenant_phone VARCHAR(20) NOT NULL,
    tenant_email VARCHAR(100),
    identity_number VARCHAR(20),
    move_in_date DATE,
    message TEXT,
    landlord_note TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE,
    FOREIGN KEY (landlord_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (tenant_user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_booking_landlord_created (landlord_id, created_at),
    INDEX idx_booking_tenant_created (tenant_user_id, created_at),
    INDEX idx_booking_room_status (room_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- 6. AUTH_VERIFICATION_TOKENS TABLE (OTP Gmail & Quen mat khau)
-- =====================================================
CREATE TABLE auth_verification_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    email VARCHAR(100) NOT NULL,
    purpose VARCHAR(30) NOT NULL,
    code_hash VARCHAR(64) NOT NULL,
    reset_ticket_hash VARCHAR(64) NULL,
    expires_at DATETIME NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 5,
    used_at DATETIME NULL,
    invalidated BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_auth_token_email_purpose_created (email, purpose, created_at),
    INDEX idx_auth_token_user_purpose (user_id, purpose)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- =====================================================
-- INSERT SAMPLE DATA
-- =====================================================

-- 1. Insert Users (password = "123456" with BCrypt)
-- BCrypt hash được generate bởi Spring Security BCryptPasswordEncoder
-- Chạy backend lần đầu với DataSeeder để tạo hash, hoặc dùng hash dưới đây:
INSERT INTO users (full_name, email, phone, password, role, is_active) VALUES
('Nguyễn Văn A', 'landlord1@example.com', '0901234567', '$2a$10$GiWwmnRbxhp0yNAQHemop.rTWz3.rueGEdUHGxks3GoY5UM/KuJfS', 'LANDLORD', TRUE),
('Trần Thị B', 'landlord2@example.com', '0912345678', '$2a$10$GiWwmnRbxhp0yNAQHemop.rTWz3.rueGEdUHGxks3GoY5UM/KuJfS', 'LANDLORD', TRUE),
('Lê Văn C', 'tenant1@example.com', '0923456789', '$2a$10$GiWwmnRbxhp0yNAQHemop.rTWz3.rueGEdUHGxks3GoY5UM/KuJfS', 'TENANT', TRUE);

-- 2. Insert Properties
INSERT INTO properties (landlord_id, name, address, description) VALUES
(1, 'Nhà trọ Hoàng Anh', '123 Nguyễn Trãi, Quận 5, TP.HCM', 'Nhà trọ cao cấp, gần trường đại học, an ninh tốt, đầy đủ tiện nghi'),
(1, 'Nhà trọ Minh Khang', '456 Lê Văn Sỹ, Quận 3, TP.HCM', 'Nhà trọ giá rẻ, an ninh tốt, gần chợ và siêu thị'),
(2, 'Nhà trọ Bình An', '789 Võ Văn Tần, Quận 3, TP.HCM', 'Nhà trọ mới xây, tiện nghi đầy đủ, view đẹp');

-- 3. Insert Rooms for Property 1 (Nhà trọ Hoàng Anh)
INSERT INTO rooms (property_id, room_code, floor, area, rent_price, deposit, capacity, amenities, description, status) VALUES
(1, '101', 1, 20.00, 2500000, 2500000, 2, 'Điều hòa, Nóng lạnh, Giường, Tủ', 'Phòng đẹp, thoáng mát, có cửa sổ lớn', 'VACANT'),
(1, '102', 1, 25.00, 3000000, 3000000, 3, 'Điều hòa, Nóng lạnh, Giường, Tủ, Ban công', 'Phòng rộng, có ban công, view đẹp', 'OCCUPIED'),
(1, '201', 2, 22.00, 2700000, 2700000, 2, 'Điều hòa, Nóng lạnh, Giường', 'Phòng tầng 2, yên tĩnh, view đường', 'EXPIRING_SOON');

-- 4. Insert Rooms for Property 2 (Nhà trọ Minh Khang)
INSERT INTO rooms (property_id, room_code, floor, area, rent_price, deposit, capacity, amenities, description, status) VALUES
(2, '1A', 1, 18.00, 2000000, 2000000, 2, 'Điều hòa, Nóng lạnh', 'Phòng giá rẻ, phù hợp sinh viên', 'VACANT'),
(2, '2B', 2, 20.00, 2300000, 2300000, 2, 'Điều hòa, Nóng lạnh, Giường, Tủ', 'Phòng đẹp, tầng 2, yên tĩnh', 'NEEDS_ATTENTION');

-- 5. Insert Rooms for Property 3 (Nhà trọ Bình An)
INSERT INTO rooms (property_id, room_code, floor, area, rent_price, deposit, capacity, amenities, description, status) VALUES
(3, 'A1', 1, 30.00, 4000000, 4000000, 4, 'Điều hòa, Nóng lạnh, Giường, Tủ, Bếp, Ban công', 'Phòng cao cấp, rộng rãi, đầy đủ tiện nghi', 'VACANT');

-- 6. Insert Tenants
INSERT INTO tenants (user_id, landlord_id, property_id, full_name, phone, email, identity_number, date_of_birth, address, emergency_contact) VALUES
(3, 1, 1, 'Lê Văn C', '0923456789', 'tenant1@example.com', '079012345678', '1995-05-15', 'Bình Dương', '0987654321'),
(NULL, 1, 2, 'Phạm Thị D', '0934567890', 'phamthid@example.com', '079123456789', '1998-08-20', 'Long An', '0976543210'),
(NULL, 2, 3, 'Hoàng Văn E', '0945678901', 'hoangvane@example.com', '079234567890', '1992-12-10', 'Đồng Nai', '0965432109');

-- =====================================================
-- VERIFY DATA
-- =====================================================
SELECT 'Users' AS Table_Name, COUNT(*) AS Record_Count FROM users
UNION ALL
SELECT 'Properties', COUNT(*) FROM properties
UNION ALL
SELECT 'Rooms', COUNT(*) FROM rooms
UNION ALL
SELECT 'Tenants', COUNT(*) FROM tenants;

-- =====================================================
-- SAMPLE QUERIES FOR TESTING
-- =====================================================

-- Xem danh sách nhà trọ của landlord 1
-- SELECT p.*, 
--        (SELECT COUNT(*) FROM rooms WHERE property_id = p.id) AS total_rooms,
--        (SELECT COUNT(*) FROM rooms WHERE property_id = p.id AND status = 'VACANT') AS vacant_rooms
-- FROM properties p 
-- WHERE p.landlord_id = 1;

-- Xem danh sách phòng theo property
-- SELECT * FROM rooms WHERE property_id = 1 ORDER BY room_code;

-- Xem danh sách khách thuê của landlord 1
-- SELECT * FROM tenants WHERE landlord_id = 1;

-- =====================================================
-- NOTES
-- =====================================================
-- 1. Password cho tất cả accounts: 123456
-- 2. Accounts có sẵn:
--    - landlord1@example.com (0901234567) - LANDLORD
--    - landlord2@example.com (0912345678) - LANDLORD  
--    - tenant1@example.com (0923456789) - TENANT
-- 3. Để import file này:
--    mysql -u root -p < roomcare_phase2.sql
-- 4. Hoặc trong MySQL Workbench:
--    File > Run SQL Script > Chọn file này
-- =====================================================
