package com.roomcare.backend.service;

import com.roomcare.backend.dto.dashboard.AttentionItemResponse;
import com.roomcare.backend.dto.dashboard.LandlordDashboardResponse;
import com.roomcare.backend.dto.dashboard.MonthlyRevenueResponse;
import com.roomcare.backend.dto.health.RoomHealthScoreResponse;
import com.roomcare.backend.entity.Invoice;
import com.roomcare.backend.entity.Room;
import com.roomcare.backend.entity.RoomStatus;
import com.roomcare.backend.repository.InvoiceRepository;
import com.roomcare.backend.repository.PropertyRepository;
import com.roomcare.backend.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PropertyRepository propertyRepository;
    private final RoomRepository roomRepository;
    private final InvoiceRepository invoiceRepository;
    private final HealthScoreService healthScoreService;

    @Transactional(readOnly = true)
    public LandlordDashboardResponse getLandlordDashboard(Long landlordId) {
        int totalProperties = (int) propertyRepository.findByLandlordId(landlordId, PageRequest.of(0, 1)).getTotalElements();
        List<Room> rooms = roomRepository.findByLandlordId(landlordId, PageRequest.of(0, 500)).getContent();
        List<Invoice> invoices = invoiceRepository.findAllByLandlordWithDetails(landlordId);

        int totalRooms = rooms.size();
        int occupiedRooms = (int) rooms.stream()
                .filter(r -> r.getStatus() == RoomStatus.OCCUPIED || r.getStatus() == RoomStatus.EXPIRING_SOON)
                .count();
        int vacantRooms = (int) rooms.stream()
                .filter(r -> r.getStatus() == RoomStatus.VACANT)
                .count();
        int expiringSoonRooms = (int) rooms.stream()
                .filter(r -> r.getStatus() == RoomStatus.EXPIRING_SOON)
                .count();
        int needsAttentionStatusCount = (int) rooms.stream()
                .filter(r -> r.getStatus() == RoomStatus.NEEDS_ATTENTION)
                .count();

        BigDecimal totalRevenue = BigDecimal.ZERO;
        BigDecimal collectedAmount = BigDecimal.ZERO;
        BigDecimal uncollectedAmount = BigDecimal.ZERO;

        Map<Long, BigDecimal> roomDebtMap = new HashMap<>();
        Map<Long, Integer> roomOverdueCountMap = new HashMap<>();
        Map<String, List<Invoice>> byMonth = new TreeMap<>();

        LocalDate today = LocalDate.now();
        for (Invoice inv : invoices) {
            totalRevenue = totalRevenue.add(inv.getTotalAmount());
            collectedAmount = collectedAmount.add(inv.getPaidAmount());
            uncollectedAmount = uncollectedAmount.add(inv.getRemainingAmount());

            Long rId = inv.getRoom().getId();
            roomDebtMap.merge(rId, inv.getRemainingAmount(), BigDecimal::add);
            if (inv.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0 && inv.getDueDate().isBefore(today)) {
                roomOverdueCountMap.merge(rId, 1, Integer::sum);
            }
            byMonth.computeIfAbsent(inv.getBillingMonth(), k -> new ArrayList<>()).add(inv);
        }

        double collectionRate = totalRevenue.compareTo(BigDecimal.ZERO) > 0
                ? collectedAmount.multiply(new BigDecimal("100"))
                .divide(totalRevenue, 1, RoundingMode.HALF_UP)
                .doubleValue()
                : 0.0;

        List<MonthlyRevenueResponse> monthlyRevenues = byMonth.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    String month = entry.getKey();
                    BigDecimal billed = entry.getValue().stream()
                            .map(Invoice::getTotalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal paid = entry.getValue().stream()
                            .map(Invoice::getPaidAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal unpaid = entry.getValue().stream()
                            .map(Invoice::getRemainingAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
                    String[] parts = month.split("-");
                    String label = parts.length == 2 ? "T" + Integer.parseInt(parts[1]) : month;
                    return new MonthlyRevenueResponse(month, label, billed, paid, unpaid);
                })
                .collect(Collectors.toList());

        if (monthlyRevenues.size() > 6) {
            monthlyRevenues = monthlyRevenues.subList(monthlyRevenues.size() - 6, monthlyRevenues.size());
        }

        List<RoomHealthScoreResponse> healthScores = rooms.stream()
                .map(healthScoreService::calculateRoomHealthScore)
                .collect(Collectors.toList());
        int averageHealthScore = healthScores.isEmpty()
                ? 0
                : (int) Math.round(healthScores.stream().mapToInt(RoomHealthScoreResponse::getTotalScore).average().orElse(0));

        Map<Long, RoomHealthScoreResponse> healthByRoomId = healthScores.stream()
                .collect(Collectors.toMap(RoomHealthScoreResponse::getRoomId, h -> h, (a, b) -> a));

        List<AttentionItemResponse> expiringContracts = new ArrayList<>();
        List<AttentionItemResponse> attentionRooms = new ArrayList<>();

        for (Room room : rooms) {
            RoomHealthScoreResponse hs = healthByRoomId.get(room.getId());
            int score = hs != null ? hs.getTotalScore() : 0;
            String tenantName = hs != null ? hs.getTenantName() : null;
            BigDecimal debt = roomDebtMap.getOrDefault(room.getId(), BigDecimal.ZERO);
            int overdueCnt = roomOverdueCountMap.getOrDefault(room.getId(), 0);

            if (room.getStatus() == RoomStatus.EXPIRING_SOON) {
                expiringContracts.add(new AttentionItemResponse(
                        room.getId(),
                        room.getRoomCode(),
                        room.getProperty().getName(),
                        tenantName,
                        "EXPIRING_SOON",
                        "Hợp đồng sắp hết hạn - cần liên hệ gia hạn",
                        score,
                        debt
                ));
            }

            if (room.getStatus() == RoomStatus.NEEDS_ATTENTION) {
                attentionRooms.add(new AttentionItemResponse(
                        room.getId(),
                        room.getRoomCode(),
                        room.getProperty().getName(),
                        tenantName,
                        "INCIDENT",
                        "Phòng đang ở trạng thái Cần chú ý" + (overdueCnt > 0 ? " & có " + overdueCnt + " hóa đơn quá hạn" : ""),
                        score,
                        debt
                ));
            } else if (overdueCnt > 0 || (hs != null && "RISK".equals(hs.getRatingLevel()))) {
                attentionRooms.add(new AttentionItemResponse(
                        room.getId(),
                        room.getRoomCode(),
                        room.getProperty().getName(),
                        tenantName,
                        overdueCnt > 0 ? "OVERDUE_DEBT" : "LOW_HEALTH_SCORE",
                        overdueCnt > 0
                                ? "Có " + overdueCnt + " hóa đơn quá hạn thanh toán"
                                : "Health Score thấp (" + score + "/100)",
                        score,
                        debt
                ));
            }
        }

        int issueRooms = Math.max(needsAttentionStatusCount, attentionRooms.size());

        return new LandlordDashboardResponse(
                totalProperties,
                totalRooms,
                occupiedRooms,
                vacantRooms,
                expiringSoonRooms,
                issueRooms,
                totalRevenue,
                collectedAmount,
                uncollectedAmount,
                collectionRate,
                averageHealthScore,
                monthlyRevenues,
                expiringContracts,
                attentionRooms
        );
    }
}
