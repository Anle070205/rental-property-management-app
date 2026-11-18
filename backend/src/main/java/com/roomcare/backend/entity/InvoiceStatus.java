package com.roomcare.backend.entity;

public enum InvoiceStatus {
    UNPAID,          // Chưa thanh toán
    PARTIALLY_PAID,  // Thanh toán một phần
    PAID,            // Đã thanh toán
    OVERDUE          // Quá hạn
}
