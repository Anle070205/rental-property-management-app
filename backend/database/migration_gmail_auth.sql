-- =====================================================
-- RoomCare Migration: Gmail Authentication, OTP Verification & Password Reset
-- An toan voi du lieu cu: cac tai khoan da ton tai mac dinh email_verified = TRUE
-- Tuong thich MySQL 8.x (khong dung ADD COLUMN IF NOT EXISTS cua MariaDB)
-- =====================================================

USE RoomCare;

-- 1. Them cot email_verified neu chua ton tai
SET @has_email_verified := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'email_verified'
);
SET @sql_email_verified := IF(
    @has_email_verified = 0,
    'ALTER TABLE users ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT TRUE AFTER is_active',
    'SELECT ''users.email_verified already exists'' AS info'
);
PREPARE stmt FROM @sql_email_verified;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 2. Them cot email_verified_at neu chua ton tai
SET @has_email_verified_at := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'email_verified_at'
);
SET @sql_email_verified_at := IF(
    @has_email_verified_at = 0,
    'ALTER TABLE users ADD COLUMN email_verified_at DATETIME NULL AFTER email_verified',
    'SELECT ''users.email_verified_at already exists'' AS info'
);
PREPARE stmt FROM @sql_email_verified_at;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 3. Them cot token_version neu chua ton tai (vo hieu hoa JWT cu sau khi doi/dat lai mat khau)
SET @has_token_version := (
    SELECT COUNT(*) FROM information_schema.COLUMNS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'users' AND COLUMN_NAME = 'token_version'
);
SET @sql_token_version := IF(
    @has_token_version = 0,
    'ALTER TABLE users ADD COLUMN token_version INT NOT NULL DEFAULT 0 AFTER email_verified_at',
    'SELECT ''users.token_version already exists'' AS info'
);
PREPARE stmt FROM @sql_token_version;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- 4. Tao bang auth_verification_tokens luu hash OTP & reset ticket (chi su dung 1 lan, co thoi han)
CREATE TABLE IF NOT EXISTS auth_verification_tokens (
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
