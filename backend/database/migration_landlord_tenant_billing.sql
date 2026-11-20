-- =====================================================
-- RoomCare Migration: Hop dong thue, Nguoi o cung,
-- Tai khoan khach thue & Vong doi Hoa don day du
-- Idempotent cho MySQL 8.x, tuyet doi khong lam mat du lieu cu
-- =====================================================

USE RoomCare;

-- 1. Bang hop dong thue phong (contracts)
CREATE TABLE IF NOT EXISTS contracts (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    contract_code VARCHAR(50) NOT NULL UNIQUE,
    landlord_id BIGINT NOT NULL,
    property_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    monthly_rent DECIMAL(15,2) NOT NULL,
    deposit_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    electricity_unit_price DECIMAL(15,2) NOT NULL DEFAULT 3500.00,
    water_unit_price DECIMAL(15,2) NOT NULL DEFAULT 15000.00,
    water_billing_mode VARCHAR(20) NOT NULL DEFAULT 'METERED',
    internet_fee DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    garbage_fee DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    service_fee DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    payment_day_of_month INT NOT NULL DEFAULT 5,
    terms TEXT,
    document_url VARCHAR(500) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
    signed_at DATETIME NULL,
    activated_at DATETIME NULL,
    terminated_at DATETIME NULL,
    termination_reason TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (landlord_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE,
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    INDEX idx_contract_landlord_status (landlord_id, status),
    INDEX idx_contract_room_status (room_id, status),
    INDEX idx_contract_tenant_status (tenant_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Bang nguoi o cung (roommates) - tach biet dai dien hop dong & tai khoan dang nhap
CREATE TABLE IF NOT EXISTS roommates (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    landlord_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    contract_id BIGINT NULL,
    full_name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) NULL,
    identity_number VARCHAR(20) NULL,
    date_of_birth DATE NULL,
    relationship VARCHAR(60) NULL,
    move_in_date DATE NULL,
    is_representative BOOLEAN NOT NULL DEFAULT FALSE,
    has_portal_access BOOLEAN NOT NULL DEFAULT FALSE,
    consent_confirmed BOOLEAN NOT NULL DEFAULT FALSE,
    notes TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (landlord_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE CASCADE,
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    FOREIGN KEY (contract_id) REFERENCES contracts(id) ON DELETE SET NULL,
    INDEX idx_roommate_tenant (tenant_id),
    INDEX idx_roommate_room (room_id),
    INDEX idx_roommate_contract (contract_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Mo rong bang auth_verification_tokens.purpose ho tro ACCOUNT_ACTIVATION
ALTER TABLE auth_verification_tokens MODIFY COLUMN channel VARCHAR(20) NOT NULL;
ALTER TABLE auth_verification_tokens MODIFY COLUMN purpose VARCHAR(30) NOT NULL;

-- 4. Nang cap idempotent bang invoices (DRAFT -> UNPAID -> PARTIALLY_PAID -> PAID / OVERDUE / CANCELLED)
ALTER TABLE invoices MODIFY COLUMN status VARCHAR(25) NOT NULL DEFAULT 'DRAFT';

-- Dam bao co index thuong tren (room_id, billing_month) truoc khi bo UNIQUE KEY cu (tranh loi FK 1553 khi huy hoa don va lap lai)
SET @has_idx_room_month := (
    SELECT COUNT(*) FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'invoices' AND INDEX_NAME = 'idx_invoice_room_month'
);
SET @sql_stmt := IF(
    @has_idx_room_month = 0,
    'CREATE INDEX idx_invoice_room_month ON invoices (room_id, billing_month)',
    'SELECT ''idx_invoice_room_month already exists'' AS info'
);
PREPARE stmt FROM @sql_stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @uk_room_month_name := (
    SELECT INDEX_NAME FROM information_schema.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'invoices'
      AND NON_UNIQUE = 0
      AND INDEX_NAME <> 'PRIMARY'
    GROUP BY INDEX_NAME
    HAVING GROUP_CONCAT(COLUMN_NAME ORDER BY SEQ_IN_INDEX) = 'room_id,billing_month'
    LIMIT 1
);
SET @sql_stmt := IF(
    @uk_room_month_name IS NOT NULL,
    CONCAT('ALTER TABLE invoices DROP INDEX `', @uk_room_month_name, '`'),
    'SELECT ''unique (room_id, billing_month) already dropped'' AS info'
);
PREPARE stmt FROM @sql_stmt;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- Helper procedure tam thoi de bo sung cot neu chua ton tai (MySQL 8 an toan)
DROP PROCEDURE IF EXISTS rc_add_col_if_missing;
DELIMITER $$
CREATE PROCEDURE rc_add_col_if_missing(
    IN p_table VARCHAR(64),
    IN p_col VARCHAR(64),
    IN p_def TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.COLUMNS
        WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = p_table AND COLUMN_NAME = p_col
    ) THEN
        SET @ddl = CONCAT('ALTER TABLE `', p_table, '` ADD COLUMN ', p_def);
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END$$
DELIMITER ;

CALL rc_add_col_if_missing('invoices', 'contract_id', 'contract_id BIGINT NULL AFTER tenant_id');
CALL rc_add_col_if_missing('invoices', 'published_at', 'published_at DATETIME NULL AFTER notes');
CALL rc_add_col_if_missing('invoices', 'cancelled_at', 'cancelled_at DATETIME NULL AFTER published_at');
CALL rc_add_col_if_missing('invoices', 'cancellation_reason', 'cancellation_reason TEXT NULL AFTER cancelled_at');
CALL rc_add_col_if_missing('invoices', 'prev_electric_index', 'prev_electric_index DECIMAL(12,2) NULL AFTER utility_record_id');
CALL rc_add_col_if_missing('invoices', 'curr_electric_index', 'curr_electric_index DECIMAL(12,2) NULL AFTER prev_electric_index');
CALL rc_add_col_if_missing('invoices', 'electric_usage', 'electric_usage DECIMAL(12,2) NULL AFTER curr_electric_index');
CALL rc_add_col_if_missing('invoices', 'electric_unit_price', 'electric_unit_price DECIMAL(15,2) NULL AFTER electric_usage');
CALL rc_add_col_if_missing('invoices', 'prev_water_index', 'prev_water_index DECIMAL(12,2) NULL AFTER electric_unit_price');
CALL rc_add_col_if_missing('invoices', 'curr_water_index', 'curr_water_index DECIMAL(12,2) NULL AFTER prev_water_index');
CALL rc_add_col_if_missing('invoices', 'water_usage', 'water_usage DECIMAL(12,2) NULL AFTER curr_water_index');
CALL rc_add_col_if_missing('invoices', 'water_unit_price', 'water_unit_price DECIMAL(15,2) NULL AFTER water_usage');
CALL rc_add_col_if_missing('invoices', 'water_billing_mode', 'water_billing_mode VARCHAR(20) NOT NULL DEFAULT ''METERED'' AFTER water_unit_price');
CALL rc_add_col_if_missing('invoices', 'occupant_count', 'occupant_count INT NOT NULL DEFAULT 1 AFTER water_billing_mode');
CALL rc_add_col_if_missing('invoices', 'garbage_fee', 'garbage_fee DECIMAL(15,2) NOT NULL DEFAULT 0.00 AFTER internet_amount');
CALL rc_add_col_if_missing('invoices', 'adjustment_amount', 'adjustment_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00 AFTER other_fees');
CALL rc_add_col_if_missing('invoices', 'adjustment_reason', 'adjustment_reason TEXT NULL AFTER adjustment_amount');
CALL rc_add_col_if_missing('invoices', 'adjustment_history', 'adjustment_history TEXT NULL AFTER adjustment_reason');
CALL rc_add_col_if_missing('invoices', 'payment_proof_note', 'payment_proof_note TEXT NULL AFTER cancellation_reason');
CALL rc_add_col_if_missing('invoices', 'payment_proof_url', 'payment_proof_url VARCHAR(500) NULL AFTER payment_proof_note');
CALL rc_add_col_if_missing('invoices', 'payment_proof_submitted_at', 'payment_proof_submitted_at DATETIME NULL AFTER payment_proof_url');

DROP PROCEDURE IF EXISTS rc_add_col_if_missing;

-- Cap nhat published_at cho hoa don da phat hanh truoc day
UPDATE invoices
SET published_at = created_at
WHERE published_at IS NULL AND status NOT IN ('DRAFT', 'CANCELLED');
