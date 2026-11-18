package com.roomcare.backend.dto.health;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HealthScoreOverviewResponse {
    private int averageScore;
    private int totalRooms;
    private int goodRoomsCount;
    private int warningRoomsCount;
    private int riskRoomsCount;
    private String formulaSummary;
    private List<RoomHealthScoreResponse> roomScores = new ArrayList<>();
}
