package com.roomcare.backend.entity;

public enum RoomStatus {
    VACANT,           // Phòng trống
    OCCUPIED,         // Đã cho thuê
    EXPIRING_SOON,    // Sắp hết hạn
    NEEDS_ATTENTION   // Cần chú ý
}
