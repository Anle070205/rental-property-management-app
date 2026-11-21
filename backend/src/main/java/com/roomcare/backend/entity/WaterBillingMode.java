package com.roomcare.backend.entity;

/**
 * Cách tính tiền nước áp dụng trên hợp đồng & hóa đơn:
 * METERED (theo chỉ số đồng hồ), FIXED (cố định hàng tháng theo phòng), PER_PERSON (theo số người ở)
 */
public enum WaterBillingMode {
    METERED,
    FIXED,
    PER_PERSON
}
