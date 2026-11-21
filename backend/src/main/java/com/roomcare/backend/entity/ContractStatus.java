package com.roomcare.backend.entity;

/**
 * Trạng thái vòng đời hợp đồng thuê phòng:
 * DRAFT (Nháp) -> SIGNED (Đã ký) -> ACTIVE (Đang hiệu lực) -> EXPIRED (Hết hạn) / TERMINATED (Đã kết thúc)
 */
public enum ContractStatus {
    DRAFT,
    SIGNED,
    ACTIVE,
    EXPIRED,
    TERMINATED
}
