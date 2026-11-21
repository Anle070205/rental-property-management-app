package com.roomcare.backend.entity;

public enum InvoiceStatus {
    DRAFT,           // Nháp (chỉ chủ trọ thấy)
    UNPAID,          // Chưa thanh toán
    PARTIALLY_PAID,  // Thanh toán một phần
    PAID,            // Đã thanh toán
    OVERDUE,         // Quá hạn
    CANCELLED        // Đã hủy
}
