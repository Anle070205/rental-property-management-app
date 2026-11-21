package com.roomcare.backend.entity;

/**
 * Role Enum - Định nghĩa các vai trò trong hệ thống
 */
public enum Role {
    LANDLORD,  // Chủ nhà - vai trò duy nhất được tự đăng ký công khai
    TENANT,    // Người thuê - chỉ do chủ nhà cấp tài khoản
    ADMIN      // Quản trị hệ thống - chỉ khởi tạo nội bộ, không đăng ký công khai
}
