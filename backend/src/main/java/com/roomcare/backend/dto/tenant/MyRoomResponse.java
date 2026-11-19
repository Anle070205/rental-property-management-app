package com.roomcare.backend.dto.tenant;

import java.math.BigDecimal;
import java.util.List;

public record MyRoomResponse(String roomCode, String address, BigDecimal rentPrice, List<String> amenities,
                             String landlordName, String landlordPhone, Integer healthScore) {
    public static MyRoomResponse empty() {
        return new MyRoomResponse(null, null, null, List.of(), null, null, null);
    }
}
