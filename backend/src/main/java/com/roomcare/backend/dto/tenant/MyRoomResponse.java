package com.roomcare.backend.dto.tenant;

import java.math.BigDecimal;
import java.util.List;

public record MyRoomResponse(
        String roomCode,
        String address,
        BigDecimal rentPrice,
        List<String> amenities,
        String landlordName,
        String landlordPhone,
        Integer healthScore,
        Long roomId,
        Long propertyId,
        Integer floor,
        BigDecimal area,
        BigDecimal deposit,
        BigDecimal electricityPrice,
        BigDecimal waterPrice,
        BigDecimal internetFee,
        BigDecimal serviceFee
) {
    public MyRoomResponse(
            String roomCode,
            String address,
            BigDecimal rentPrice,
            List<String> amenities,
            String landlordName,
            String landlordPhone,
            Integer healthScore
    ) {
        this(roomCode, address, rentPrice, amenities, landlordName, landlordPhone, healthScore,
                null, null, null, null, null, null, null, null, null);
    }

    public static MyRoomResponse empty() {
        return new MyRoomResponse(null, null, null, List.of(), null, null, null,
                null, null, null, null, null, null, null, null, null);
    }
}
