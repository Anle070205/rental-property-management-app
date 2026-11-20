-- =====================================================
-- RoomCare Migration: Xac thuc Email (Gmail) hoac So dien thoai (OTP SMS)
-- An toan voi du lieu cu:
--   - Khong xoa du lieu nguoi dung hien tai
--   - Cac tai khoan cu mac dinh email_verified = TRUE va phone_verified = TRUE
--   - Cho phep email hoac phone NULL (tai khoan chi dang ky 1 trong 2 phuong thuc)
-- =====================================================

USE RoomCare;

-- 1. Cho phep users.email nullable de ho tro tai khoan chi dang ky bang SDT
ALTER TABLE users MODIFY COLUMN email VARCHAR(100) NULL;

-- 2. Cho phep users.phone nullable va tang do dai len 20 ky tu de luu chuan E.164 (+84...)
ALTER TABLE users MODIFY COLUMN phone VARCHAR(20) NULL;

-- 3. Cho phep tenants.phone va booking_requests.tenant_phone nullable khi nguoi dung chi dang ky bang Email
ALTER TABLE tenants MODIFY COLUMN phone VARCHAR(20) NULL;
ALTER TABLE booking_requests MODIFY COLUMN tenant_phone VARCHAR(20) NULL;

-- 4. Them cac cot trang thai xac thuc & token_version cho users (MySQL 8 idempotent)
SET @has_email_verified := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'email_verified'
);
SET @sql_stmt := IF(
    @has_email_verified = 0,
    'ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT TRUE AFTER is_active',
    'SELECT ''users.email_verified already exists'' AS info'
);
PREPARE stmt FROM @sql_stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_email_verified_at := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'email_verified_at'
);
SET @sql_stmt := IF(
    @has_email_verified_at = 0,
    'ALTER TABLE users ADD COLUMN email_verified_at DATETIME NULL AFTER email_verified',
    'SELECT ''users.email_verified_at already exists'' AS info'
);
PREPARE stmt FROM @sql_stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_phone_verified := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'phone_verified'
);
SET @sql_stmt := IF(
    @has_phone_verified = 0,
    'ALTER TABLE users ADD COLUMN phone_verified BOOLEAN NOT NULL DEFAULT TRUE AFTER email_verified_at',
    'SELECT ''users.phone_verified already exists'' AS info'
);
PREPARE stmt FROM @sql_stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_phone_verified_at := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'phone_verified_at'
);
SET @sql_stmt := IF(
    @has_phone_verified_at = 0,
    'ALTER TABLE users ADD COLUMN phone_verified_at DATETIME NULL AFTER phone_verified',
    'SELECT ''users.phone_verified_at already exists'' AS info'
);
PREPARE stmt FROM @sql_stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @has_token_version := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'token_version'
);
SET @sql_stmt := IF(
    @has_token_version = 0,
    'ALTER TABLE users ADD COLUMN token_version INT NOT NULL DEFAULT 0 AFTER phone_verified_at',
    'SELECT ''users.token_version already exists'' AS info'
);
PREPARE stmt FROM @sql_stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 5. Tao bang auth_verification_tokens luu OTP (hash SHA-256 / provider_managed)
CREATE TABLE IF NOT EXISTS auth_verification_tokens (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    channel VARCHAR(20) NOT NULL,
    purpose VARCHAR(30) NOT NULL,
    target VARCHAR(100) NOT NULL,
    code_hash VARCHAR(64) NOT NULL,
    reset_ticket_hash VARCHAR(64) NULL,
    provider VARCHAR(30) NOT NULL DEFAULT 'LOCAL_HASH',
    provider_reference VARCHAR(120) NULL,
    client_ip VARCHAR(64) NULL,
    expires_at DATETIME NOT NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 5,
    used_at DATETIME NULL,
    invalidated BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_otp_target_channel_purpose_created (target, channel, purpose, created_at),
    INDEX idx_otp_user_channel_purpose (user_id, channel, purpose),
    INDEX idx_otp_ip_channel_created (client_ip, channel, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
