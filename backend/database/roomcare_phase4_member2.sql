-- =====================================================
-- RoomCare Database - Phase 4 (Member 2: Finance, Utilities & Health Score)
-- Bổ sung 3 bảng mới: utility_records, invoices, payments
-- Lưu ý: Chạy sau roomcare_phase2.sql (hoặc dùng Hibernate ddl-auto=update)
-- =====================================================

USE RoomCare;

CREATE TABLE IF NOT EXISTS utility_records (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    landlord_id BIGINT NOT NULL,
    property_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    billing_month VARCHAR(7) NOT NULL,
    prev_electric_index DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    curr_electric_index DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    electric_usage DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    electric_unit_price DECIMAL(15,2) NOT NULL DEFAULT 3500.00,
    electricity_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    prev_water_index DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    curr_water_index DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    water_usage DECIMAL(12,2) NOT NULL DEFAULT 0.00,
    water_unit_price DECIMAL(15,2) NOT NULL DEFAULT 15000.00,
    water_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    internet_fee DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    service_fee DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    total_utility_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    is_adjustment BOOLEAN NOT NULL DEFAULT FALSE,
    adjustment_reason TEXT,
    notes TEXT,
    history_log TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (landlord_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE,
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    UNIQUE KEY uk_utility_room_month (room_id, billing_month),
    INDEX idx_utility_landlord_month (landlord_id, billing_month)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS invoices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_code VARCHAR(40) NOT NULL UNIQUE,
    landlord_id BIGINT NOT NULL,
    property_id BIGINT NOT NULL,
    room_id BIGINT NOT NULL,
    tenant_id BIGINT NULL,
    utility_record_id BIGINT NULL,
    billing_month VARCHAR(7) NOT NULL,
    room_rent DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    electricity_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    water_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    internet_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    service_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    other_fees DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    previous_debt DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    discount_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    total_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    paid_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    remaining_amount DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    status VARCHAR(20) NOT NULL DEFAULT 'UNPAID',
    due_date DATE NOT NULL,
    notes TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (landlord_id) REFERENCES users(id) ON DELETE CASCADE,
    FOREIGN KEY (property_id) REFERENCES properties(id) ON DELETE CASCADE,
    FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
    FOREIGN KEY (tenant_id) REFERENCES tenants(id) ON DELETE SET NULL,
    UNIQUE KEY uk_invoice_room_month (room_id, billing_month),
    INDEX idx_invoice_landlord_month (landlord_id, billing_month),
    INDEX idx_invoice_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS payments (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    invoice_id BIGINT NOT NULL,
    landlord_id BIGINT NOT NULL,
    amount DECIMAL(15,2) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    transaction_code VARCHAR(100),
    receipt_note TEXT,
    paid_at DATETIME NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE,
    FOREIGN KEY (landlord_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_payment_invoice (invoice_id),
    INDEX idx_payment_tx_code (transaction_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
