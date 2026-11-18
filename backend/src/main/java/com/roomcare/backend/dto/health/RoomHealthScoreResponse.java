package com.roomcare.backend.dto.health;

import com.roomcare.backend.entity.RoomStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoomHealthScoreResponse {
    private Long roomId;
    private String roomCode;
    private Long propertyId;
    private String propertyName;
    private RoomStatus roomStatus;
    private String tenantName;
    private int totalScore;
    private String ratingLevel; // GOOD | WARNING | RISK
    private String ratingLabel;
    private String trend;       // UP | STABLE | DOWN
    private int scoreDelta;
    private List<HealthFactorResponse> factors = new ArrayList<>();
    private List<String> reasonsForDeduction = new ArrayList<>();
    private List<String> recommendations = new ArrayList<>();
    private String formulaNote;
}
