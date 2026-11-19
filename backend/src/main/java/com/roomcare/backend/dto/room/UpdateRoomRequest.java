package com.roomcare.backend.dto.room;

import com.roomcare.backend.entity.RoomStatus;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateRoomRequest {
    
    @NotBlank(message = "Mã phòng không được để trống")
    @Size(max = 50, message = "Mã phòng không được vượt quá 50 ký tự")
    private String roomCode;
    
    @NotNull(message = "Tầng không được để trống")
    @Min(value = 0, message = "Tầng phải lớn hơn hoặc bằng 0")
    private Integer floor;
    
    @NotNull(message = "Diện tích không được để trống")
    @DecimalMin(value = "0.01", message = "Diện tích phải lớn hơn 0")
    private BigDecimal area;
    
    @NotNull(message = "Giá thuê không được để trống")
    @DecimalMin(value = "0.01", message = "Giá thuê phải lớn hơn 0")
    private BigDecimal rentPrice;
    
    @NotNull(message = "Tiền đặt cọc không được để trống")
    @DecimalMin(value = "0.00", message = "Tiền đặt cọc phải lớn hơn hoặc bằng 0")
    private BigDecimal deposit;
    
    @NotNull(message = "Sức chứa không được để trống")
    @Min(value = 1, message = "Sức chứa phải lớn hơn 0")
    private Integer capacity;
    
    private String amenities;
    
    @Size(max = 1000, message = "Mô tả không được vượt quá 1000 ký tự")
    private String description;
    
    private String imageUrl;
    
    @NotNull(message = "Trạng thái không được để trống")
    private RoomStatus status;

    @DecimalMin(value = "0.00", message = "Đơn giá điện phải lớn hơn hoặc bằng 0")
    private BigDecimal electricityPrice;

    @DecimalMin(value = "0.00", message = "Đơn giá nước phải lớn hơn hoặc bằng 0")
    private BigDecimal waterPrice;

    @DecimalMin(value = "0.00", message = "Phí internet phải lớn hơn hoặc bằng 0")
    private BigDecimal internetFee;

    @DecimalMin(value = "0.00", message = "Phí dịch vụ phải lớn hơn hoặc bằng 0")
    private BigDecimal serviceFee;
}
