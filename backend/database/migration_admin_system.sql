-- RoomCare Admin System Migration (Idempotent for MySQL 8.x)
-- Preserves all existing users, properties, rooms, tenants, contracts, invoices, incidents, and notifications.

-- 1. Extend users.role enum to support ADMIN (internal provisioned only, never public signup)
ALTER TABLE users
    MODIFY COLUMN role ENUM('LANDLORD', 'TENANT', 'ADMIN') NOT NULL;

-- 2. Create admin_broadcasts table for system & role-targeted announcements (DRAFT -> SENT)
CREATE TABLE IF NOT EXISTS admin_broadcasts (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    broadcast_code VARCHAR(30) NOT NULL UNIQUE,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    target_audience ENUM('ALL', 'ADMIN', 'LANDLORD', 'TENANT') NOT NULL DEFAULT 'ALL',
    status ENUM('DRAFT', 'SENT') NOT NULL DEFAULT 'DRAFT',
    recipient_count INT NOT NULL DEFAULT 0,
    created_by_id BIGINT NULL,
    created_by_name VARCHAR(100) NULL,
    sent_at DATETIME(6) NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    INDEX idx_admin_broadcasts_status (status, updated_at),
    CONSTRAINT fk_admin_broadcast_creator FOREIGN KEY (created_by_id) REFERENCES users (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Add optional Admin resolution audit columns to incidents (safe idempotent procedure)
DROP PROCEDURE IF EXISTS roomcare_add_col_if_missing;
DELIMITER $$
CREATE PROCEDURE roomcare_add_col_if_missing(
    IN p_table VARCHAR(64),
    IN p_col VARCHAR(64),
    IN p_def VARCHAR(255)
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND COLUMN_NAME = p_col
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE ', p_table, ' ADD COLUMN ', p_col, ' ', p_def);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL roomcare_add_col_if_missing('incidents', 'resolved_by_id', 'BIGINT NULL');
CALL roomcare_add_col_if_missing('incidents', 'resolved_by_name', 'VARCHAR(100) NULL');
CALL roomcare_add_col_if_missing('incidents', 'resolved_at', 'DATETIME(6) NULL');

DROP PROCEDURE IF EXISTS roomcare_add_col_if_missing;
