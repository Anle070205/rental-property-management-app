package com.roomcare.backend.service;

import com.roomcare.backend.dto.health.HealthFactorResponse;
import com.roomcare.backend.dto.health.HealthScoreOverviewResponse;
import com.roomcare.backend.dto.health.RoomHealthScoreResponse;
import com.roomcare.backend.entity.*;
import com.roomcare.backend.repository.InvoiceRepository;
import com.roomcare.backend.repository.RoomRepository;
import com.roomcare.backend.repository.UtilityRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HealthScoreService {

    public static final String FORMULA_SUMMARY =
            "Công thức đề xuất (chờ nhóm thống nhất): Thanh toán đúng hạn (40đ) + Hợp đồng & Lấp đầy (25đ) + Điện nước ổn định (20đ) + Sự cố & Vận hành (15đ). Thiếu dữ liệu áp dụng điểm cơ sở trung lập.";

    private final RoomRepository roomRepository;
    private final InvoiceRepository invoiceRepository;
    private final UtilityRecordRepository utilityRecordRepository;

    @Transactional(readOnly = true)
    public HealthScoreOverviewResponse getAllRoomHealthScores(
            Long landlordId,
            String ratingLevel,
            Long propertyId) {

        List<Room> rooms = roomRepository.findByLandlordId(landlordId, PageRequest.of(0, 500)).getContent();
        if (propertyId != null) {
            rooms = rooms.stream()
                    .filter(r -> r.getProperty().getId().equals(propertyId))
                    .collect(Collectors.toList());
        }

        List<RoomHealthScoreResponse> allScores = rooms.stream()
                .map(this::calculateRoomHealthScore)
                .sorted(Comparator.comparingInt(RoomHealthScoreResponse::getTotalScore))
                .collect(Collectors.toList());

        int goodCount = (int) allScores.stream().filter(s -> "GOOD".equals(s.getRatingLevel())).count();
        int warningCount = (int) allScores.stream().filter(s -> "WARNING".equals(s.getRatingLevel())).count();
        int riskCount = (int) allScores.stream().filter(s -> "RISK".equals(s.getRatingLevel())).count();
        int average = allScores.isEmpty()
                ? 0
                : (int) Math.round(allScores.stream().mapToInt(RoomHealthScoreResponse::getTotalScore).average().orElse(0));

        List<RoomHealthScoreResponse> filtered = allScores;
        if (ratingLevel != null && !ratingLevel.isBlank()) {
            String target = ratingLevel.trim().toUpperCase();
            filtered = allScores.stream()
                    .filter(s -> target.equals(s.getRatingLevel()))
                    .collect(Collectors.toList());
        }

        return new HealthScoreOverviewResponse(
                average,
                allScores.size(),
                goodCount,
                warningCount,
                riskCount,
                FORMULA_SUMMARY,
                filtered
        );
    }

    @Transactional(readOnly = true)
    public RoomHealthScoreResponse getRoomHealthScore(Long landlordId, Long roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));

        if (!room.getProperty().getLandlord().getId().equals(landlordId)) {
            throw new RuntimeException("Bạn không có quyền xem Health Score của phòng này");
        }

        return calculateRoomHealthScore(room);
    }

    public RoomHealthScoreResponse calculateRoomHealthScore(Room room) {
        List<Invoice> invoices = invoiceRepository.findByRoomIdOrderByBillingMonthDesc(room.getId());
        List<UtilityRecord> utilities = utilityRecordRepository.findByRoomIdOrderByBillingMonthDesc(room.getId());

        List<String> deductions = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        LocalDate today = LocalDate.now();

        String tenantName = invoices.stream()
                .filter(i -> i.getTenant() != null)
                .map(i -> i.getTenant().getFullName())
                .findFirst()
                .orElse(null);

        // 1. Lịch sử thanh toán đúng hạn (40đ)
        int paymentScore;
        String paymentDetail;
        int overdueCount = 0;
        if (invoices.isEmpty()) {
            paymentScore = 35;
            paymentDetail = "Chưa có hóa đơn phát sinh (áp dụng điểm cơ sở 35/40)";
            recommendations.add("Lập hóa đơn định kỳ để theo dõi chính xác lịch sử thanh toán");
        } else {
            paymentScore = 40;
            int partialCount = 0;
            int unpaidNotDueCount = 0;
            long maxOverdueDays = 0;

            for (Invoice inv : invoices) {
                boolean hasRemaining = inv.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0;
                boolean isOverdue = hasRemaining && (inv.getDueDate().isBefore(today) || inv.getStatus() == InvoiceStatus.OVERDUE);
                if (isOverdue) {
                    overdueCount++;
                    paymentScore -= 12;
                    long days = Math.max(1, ChronoUnit.DAYS.between(inv.getDueDate(), today));
                    if (days > maxOverdueDays) {
                        maxOverdueDays = days;
                    }
                } else if (hasRemaining && inv.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
                    partialCount++;
                    paymentScore -= 5;
                } else if (hasRemaining) {
                    unpaidNotDueCount++;
                    paymentScore -= 2;
                }
            }

            if (maxOverdueDays > 15) {
                paymentScore -= 8;
            }
            paymentScore = Math.max(0, Math.min(40, paymentScore));

            if (overdueCount > 0) {
                deductions.add("Có " + overdueCount + " hóa đơn quá hạn (trễ tối đa " + maxOverdueDays + " ngày)");
                recommendations.add("Nhắc nhở thanh toán và đối chiếu công nợ ngay cho phòng " + room.getRoomCode());
            }
            if (partialCount > 0) {
                deductions.add("Có " + partialCount + " hóa đơn mới thanh toán một phần");
                recommendations.add("Theo dõi phần tiền còn thiếu của các hóa đơn đã thu một phần");
            }
            if (overdueCount == 0 && partialCount == 0 && unpaidNotDueCount == 0) {
                paymentDetail = "Thanh toán đầy đủ và đúng hạn toàn bộ " + invoices.size() + " hóa đơn";
            } else {
                paymentDetail = overdueCount + " quá hạn, " + partialCount + " trả 1 phần, " + unpaidNotDueCount + " chờ thu";
            }
        }

        // 2. Tình trạng hợp đồng & khai thác phòng (25đ)
        int contractScore;
        String contractDetail;
        switch (room.getStatus()) {
            case OCCUPIED -> {
                contractScore = 25;
                contractDetail = "Phòng đang thuê ổn định";
            }
            case EXPIRING_SOON -> {
                contractScore = 15;
                contractDetail = "Hợp đồng sắp hết hạn (cần gia hạn)";
                deductions.add("Hợp đồng phòng " + room.getRoomCode() + " sắp hết hạn");
                recommendations.add("Trao đổi với người thuê để ký gia hạn hợp đồng kịp thời");
            }
            case VACANT -> {
                contractScore = 12;
                contractDetail = "Phòng đang trống";
                deductions.add("Phòng đang trống, chưa tạo dòng tiền thuê");
                recommendations.add("Cập nhật thông tin cho thuê để nhanh chóng lấp đầy phòng");
            }
            case NEEDS_ATTENTION -> {
                contractScore = 8;
                contractDetail = "Phòng ở trạng thái Cần chú ý";
                deductions.add("Trạng thái phòng đang đánh dấu Cần chú ý");
            }
            default -> {
                contractScore = 15;
                contractDetail = "Không xác định";
            }
        }

        // 3. Ổn định điện nước & ghi nhận định kỳ (20đ)
        int utilityScore;
        String utilityDetail;
        if (room.getStatus() == RoomStatus.VACANT || utilities.isEmpty()) {
            utilityScore = 16;
            utilityDetail = "Chưa có đủ dữ liệu chỉ số điện nước (áp dụng điểm cơ sở 16/20)";
            if (room.getStatus() != RoomStatus.VACANT) {
                deductions.add("Chưa ghi chỉ số điện nước cho phòng đang thuê");
                recommendations.add("Ghi nhận chỉ số điện nước kỳ hiện tại trong mục Điện, nước & dịch vụ");
            }
        } else {
            utilityScore = 20;
            UtilityRecord latest = utilities.get(0);
            String currentMonth = YearMonth.now().toString();
            if ((room.getStatus() == RoomStatus.OCCUPIED || room.getStatus() == RoomStatus.EXPIRING_SOON)
                    && latest.getBillingMonth().compareTo(currentMonth) < 0) {
                utilityScore -= 8;
                deductions.add("Chưa ghi chỉ số điện nước kỳ " + currentMonth + " (mới nhất: " + latest.getBillingMonth() + ")");
                recommendations.add("Chốt chỉ số điện nước kỳ " + currentMonth + " trước khi lập hóa đơn");
            }

            if (utilities.size() >= 2) {
                UtilityRecord prev = utilities.get(1);
                boolean electricSpike = isSpikeOver50Percent(prev.getElectricUsage(), latest.getElectricUsage());
                boolean waterSpike = isSpikeOver50Percent(prev.getWaterUsage(), latest.getWaterUsage());
                if (electricSpike || waterSpike) {
                    utilityScore -= 6;
                    deductions.add("Tiêu thụ điện/nước kỳ " + latest.getBillingMonth() + " tăng đột biến > 50%");
                    recommendations.add("Kiểm tra rò rỉ nước hoặc thiết bị tiêu thụ điện bất thường");
                }
            }

            if (Boolean.TRUE.equals(latest.getIsAdjustment())) {
                utilityScore -= 4;
                deductions.add("Có điều chỉnh chỉ số đồng hồ cần theo dõi");
            }

            utilityScore = Math.max(0, Math.min(20, utilityScore));
            utilityDetail = "Kỳ gần nhất " + latest.getBillingMonth()
                    + ": " + latest.getElectricUsage().stripTrailingZeros().toPlainString() + " kWh, "
                    + latest.getWaterUsage().stripTrailingZeros().toPlainString() + " m³";
        }

        // 4. Sự cố & vận hành (15đ)
        int incidentScore = 15;
        String incidentDetail = "Vận hành bình thường, không ghi nhận sự cố";
        if (room.getStatus() == RoomStatus.NEEDS_ATTENTION) {
            incidentScore -= 10;
            deductions.add("Phòng đang có cảnh báo sự cố / cần bảo trì");
            recommendations.add("Kiểm tra thực tế phòng và khắc phục sự cố trước kỳ thu mới");
        }
        if (overdueCount >= 2) {
            incidentScore -= 5;
            deductions.add("Rủi ro công nợ kéo dài từ " + overdueCount + " kỳ hóa đơn");
            recommendations.add("Lập kế hoạch thu hồi công nợ hoặc làm việc trực tiếp với người thuê");
        }
        incidentScore = Math.max(0, Math.min(15, incidentScore));
        if (incidentScore < 15) {
            incidentDetail = "Cần xử lý cảnh báo vận hành / công nợ";
        }

        int totalScore = Math.max(0, Math.min(100, paymentScore + contractScore + utilityScore + incidentScore));
        String ratingLevel;
        String ratingLabel;
        if (totalScore >= 80) {
            ratingLevel = "GOOD";
            ratingLabel = "Tốt";
        } else if (totalScore >= 50) {
            ratingLevel = "WARNING";
            ratingLabel = "Cần theo dõi";
        } else {
            ratingLevel = "RISK";
            ratingLabel = "Rủi ro";
        }

        // Xu hướng (Trend) dựa trên trạng thái kỳ mới nhất so với tổng thể
        String trend = "STABLE";
        int scoreDelta = 0;
        if (overdueCount > 0 || room.getStatus() == RoomStatus.NEEDS_ATTENTION) {
            trend = "DOWN";
            scoreDelta = -Math.max(4, overdueCount * 5);
        } else if (!invoices.isEmpty() && invoices.get(0).getStatus() == InvoiceStatus.PAID) {
            trend = "UP";
            scoreDelta = 4;
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Duy trì lịch ghi chỉ số điện nước và thu tiền đúng hạn như hiện tại");
        }

        List<HealthFactorResponse> factors = List.of(
                new HealthFactorResponse(
                        "Thanh toán đúng hạn",
                        paymentScore,
                        40,
                        40,
                        factorStatus(paymentScore, 40),
                        paymentDetail
                ),
                new HealthFactorResponse(
                        "Hợp đồng & Lấp đầy",
                        contractScore,
                        25,
                        25,
                        factorStatus(contractScore, 25),
                        contractDetail
                ),
                new HealthFactorResponse(
                        "Ổn định điện nước",
                        utilityScore,
                        20,
                        20,
                        factorStatus(utilityScore, 20),
                        utilityDetail
                ),
                new HealthFactorResponse(
                        "Sự cố & Vận hành",
                        incidentScore,
                        15,
                        15,
                        factorStatus(incidentScore, 15),
                        incidentDetail
                )
        );

        return new RoomHealthScoreResponse(
                room.getId(),
                room.getRoomCode(),
                room.getProperty().getId(),
                room.getProperty().getName(),
                room.getStatus(),
                tenantName,
                totalScore,
                ratingLevel,
                ratingLabel,
                trend,
                scoreDelta,
                factors,
                deductions,
                recommendations,
                FORMULA_SUMMARY
        );
    }

    private boolean isSpikeOver50Percent(BigDecimal previous, BigDecimal current) {
        if (previous == null || current == null || previous.compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        return current.compareTo(previous.multiply(new BigDecimal("1.5"))) > 0;
    }

    private String factorStatus(int score, int max) {
        double ratio = max == 0 ? 1.0 : (double) score / max;
        if (ratio >= 0.8) return "GOOD";
        if (ratio >= 0.5) return "WARNING";
        return "RISK";
    }
}
