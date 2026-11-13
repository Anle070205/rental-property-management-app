package com.roomcare.backend.dto.room;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateRoomRequest {
    
    @NotNull(message = "ID nhà trọ không được để trống")
    private Long propertyId;
    
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
}
