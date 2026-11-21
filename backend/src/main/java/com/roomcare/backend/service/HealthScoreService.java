package com.roomcare.backend.service;

import com.roomcare.backend.dto.health.HealthFactorResponse;
import com.roomcare.backend.dto.health.HealthScoreOverviewResponse;
import com.roomcare.backend.dto.health.RoomHealthScoreResponse;
import com.roomcare.backend.entity.*;
import com.roomcare.backend.repository.BookingRequestRepository;
import com.roomcare.backend.repository.IncidentRepository;
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
            "Mặc định 100đ cho phòng mới. Gồm: Tình trạng & Sự cố/Hư hỏng (40đ) + Thanh toán đúng hạn (30đ) + Điện nước ổn định (15đ) + Hợp đồng & Vận hành (15đ). Trừ điểm khi phát sinh hư hỏng/sự cố, nợ quá hạn hoặc bất thường.";

    private final RoomRepository roomRepository;
    private final InvoiceRepository invoiceRepository;
    private final UtilityRecordRepository utilityRecordRepository;
    private final IncidentRepository incidentRepository;
    private final BookingRequestRepository bookingRequestRepository;

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

    @Transactional
    public int syncRoomHealthScore(Room room) {
        if (room == null) {
            return 100;
        }
        Room managed = room.getId() != null
                ? roomRepository.findById(room.getId()).orElse(room)
                : room;
        int score = calculateRoomHealthScore(managed).getTotalScore();
        managed.setHealthScore(score);
        roomRepository.save(managed);
        room.setHealthScore(score);
        return score;
    }

    public RoomHealthScoreResponse calculateRoomHealthScore(Room room) {
        List<Invoice> rawInvoices = invoiceRepository != null
                ? invoiceRepository.findByRoomIdOrderByBillingMonthDesc(room.getId())
                : List.of();
        List<Invoice> invoices = rawInvoices == null
                ? List.of()
                : rawInvoices.stream()
                        .filter(inv -> inv.getStatus() != InvoiceStatus.DRAFT && inv.getStatus() != InvoiceStatus.CANCELLED)
                        .collect(Collectors.toList());
        List<UtilityRecord> utilities = utilityRecordRepository != null
                ? utilityRecordRepository.findByRoomIdOrderByBillingMonthDesc(room.getId())
                : List.of();
        if (utilities == null) {
            utilities = List.of();
        }
        List<Incident> incidents = incidentRepository != null
                ? incidentRepository.findByRoomIdOrderByCreatedAtDesc(room.getId())
                : List.of();
        if (incidents == null) {
            incidents = List.of();
        }

        List<String> deductions = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();
        LocalDate today = LocalDate.now();

        String tenantName = invoices.stream()
                .filter(i -> i.getTenant() != null)
                .map(i -> i.getTenant().getFullName())
                .findFirst()
                .orElse(null);
        if (tenantName == null && bookingRequestRepository != null) {
            tenantName = bookingRequestRepository
                    .findFirstByRoomIdAndRequestTypeAndStatusOrderByUpdatedAtDesc(
                            room.getId(),
                            BookingRequest.RequestType.BOOKING,
                            BookingRequest.Status.APPROVED
                    )
                    .map(BookingRequest::getTenantName)
                    .orElse(null);
        }

        // 1. Tình trạng phòng & Sự cố/Hư hỏng (40đ) - làm cơ sở đối chiếu với người thuê
        int incidentScore = 40;
        int openIncidentCount = 0;
        for (Incident inc : incidents) {
            if (inc.getStatus() != Incident.Status.DONE) {
                openIncidentCount++;
                int penalty = switch (inc.getPriority()) {
                    case URGENT -> 15;
                    case HIGH -> 10;
                    case MEDIUM -> 6;
                    case LOW -> 3;
                };
                incidentScore -= penalty;
                deductions.add("Hư hỏng/Sự cố (" + inc.getPriority().name() + ", -" + penalty + "đ): " + inc.getTitle());
                recommendations.add("Kiểm tra thực tế và xử lý sự cố \"" + inc.getTitle() + "\" cho phòng " + room.getRoomCode());
            }
        }
        if (openIncidentCount == 0 && room.getStatus() == RoomStatus.NEEDS_ATTENTION) {
            incidentScore -= 15;
            deductions.add("Phòng đang được đánh dấu Cần chú ý / có vấn đề cần kiểm tra (-15đ)");
            recommendations.add("Kiểm tra thực tế phòng " + room.getRoomCode() + " và khắc phục sự cố trước kỳ thu mới");
        }
        incidentScore = Math.max(0, Math.min(40, incidentScore));
        String incidentDetail;
        if (openIncidentCount > 0) {
            incidentDetail = "Có " + openIncidentCount + " sự cố/hư hỏng chưa hoàn tất";
        } else if (room.getStatus() == RoomStatus.NEEDS_ATTENTION) {
            incidentDetail = "Phòng ở trạng thái Cần chú ý";
        } else {
            incidentDetail = "Phòng nguyên trạng, không có báo cáo hư hỏng/sự cố";
        }

        // 2. Thanh toán đúng hạn (30đ)
        int paymentScore;
        String paymentDetail;
        int overdueCount = 0;
        if (invoices.isEmpty()) {
            paymentScore = 30;
            paymentDetail = "Chưa phát sinh công nợ quá hạn (mặc định 30/30)";
        } else {
            paymentScore = 30;
            int partialCount = 0;
            int unpaidNotDueCount = 0;
            long maxOverdueDays = 0;

            for (Invoice inv : invoices) {
                boolean hasRemaining = inv.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0;
                boolean isOverdue = hasRemaining && (inv.getDueDate().isBefore(today) || inv.getStatus() == InvoiceStatus.OVERDUE);
                if (isOverdue) {
                    overdueCount++;
                    paymentScore -= 10;
                    long days = Math.max(1, ChronoUnit.DAYS.between(inv.getDueDate(), today));
                    if (days > maxOverdueDays) {
                        maxOverdueDays = days;
                    }
                } else if (hasRemaining && inv.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
                    partialCount++;
                    paymentScore -= 4;
                } else if (hasRemaining) {
                    unpaidNotDueCount++;
                    paymentScore -= 2;
                }
            }

            if (maxOverdueDays > 15) {
                paymentScore -= 6;
            }
            paymentScore = Math.max(0, Math.min(30, paymentScore));

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

        // 3. Ổn định điện nước & ghi nhận định kỳ (15đ)
        int utilityScore;
        String utilityDetail;
        if (room.getStatus() == RoomStatus.VACANT || utilities.isEmpty()) {
            utilityScore = 15;
            utilityDetail = "Chưa ghi nhận bất thường điện nước (mặc định 15/15)";
        } else {
            utilityScore = 15;
            UtilityRecord latest = utilities.get(0);
            String currentMonth = YearMonth.now().toString();
            if ((room.getStatus() == RoomStatus.OCCUPIED || room.getStatus() == RoomStatus.EXPIRING_SOON)
                    && latest.getBillingMonth().compareTo(currentMonth) < 0) {
                utilityScore -= 5;
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

            utilityScore = Math.max(0, Math.min(15, utilityScore));
            utilityDetail = "Kỳ gần nhất " + latest.getBillingMonth()
                    + ": " + latest.getElectricUsage().stripTrailingZeros().toPlainString() + " kWh, "
                    + latest.getWaterUsage().stripTrailingZeros().toPlainString() + " m³";
        }

        // 4. Hợp đồng & vận hành (15đ)
        int contractScore;
        String contractDetail;
        switch (room.getStatus()) {
            case VACANT -> {
                contractScore = 15;
                contractDetail = "Phòng trống sẵn sàng bàn giao (15/15)";
            }
            case OCCUPIED -> {
                contractScore = 15;
                contractDetail = "Phòng đang thuê ổn định";
            }
            case EXPIRING_SOON -> {
                contractScore = 10;
                contractDetail = "Hợp đồng sắp hết hạn (cần gia hạn)";
                deductions.add("Hợp đồng phòng " + room.getRoomCode() + " sắp hết hạn");
                recommendations.add("Trao đổi với người thuê để ký gia hạn hợp đồng kịp thời");
            }
            case NEEDS_ATTENTION -> {
                contractScore = 5;
                contractDetail = "Phòng ở trạng thái Cần chú ý";
                deductions.add("Trạng thái phòng đang đánh dấu Cần chú ý");
            }
            default -> {
                contractScore = 15;
                contractDetail = "Ổn định";
            }
        }
        if (overdueCount >= 2) {
            contractScore -= 5;
            deductions.add("Rủi ro công nợ kéo dài từ " + overdueCount + " kỳ hóa đơn");
            recommendations.add("Lập kế hoạch thu hồi công nợ hoặc làm việc trực tiếp với người thuê");
        }
        contractScore = Math.max(0, Math.min(15, contractScore));

        int totalScore = Math.max(0, Math.min(100, incidentScore + paymentScore + utilityScore + contractScore));
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

        // Xu hướng (Trend)
        String trend = "STABLE";
        int scoreDelta = 0;
        if (openIncidentCount > 0 || overdueCount > 0 || room.getStatus() == RoomStatus.NEEDS_ATTENTION) {
            trend = "DOWN";
            scoreDelta = -(100 - totalScore == 0 ? 5 : Math.min(35, 100 - totalScore));
        } else if (!invoices.isEmpty() && invoices.get(0).getStatus() == InvoiceStatus.PAID) {
            trend = "UP";
            scoreDelta = 4;
        }

        if (recommendations.isEmpty()) {
            recommendations.add(totalScore == 100
                    ? "Phòng đạt điểm chuẩn 100/100; đối chiếu tình trạng phòng nếu phát sinh hư hỏng từ người thuê."
                    : "Duy trì lịch ghi chỉ số điện nước và thu tiền đúng hạn như hiện tại");
        }

        List<HealthFactorResponse> factors = List.of(
                new HealthFactorResponse(
                        "Tình trạng & Sự cố",
                        incidentScore,
                        40,
                        40,
                        factorStatus(incidentScore, 40),
                        incidentDetail
                ),
                new HealthFactorResponse(
                        "Thanh toán đúng hạn",
                        paymentScore,
                        30,
                        30,
                        factorStatus(paymentScore, 30),
                        paymentDetail
                ),
                new HealthFactorResponse(
                        "Ổn định điện nước",
                        utilityScore,
                        15,
                        15,
                        factorStatus(utilityScore, 15),
                        utilityDetail
                ),
                new HealthFactorResponse(
                        "Hợp đồng & Vận hành",
                        contractScore,
                        15,
                        15,
                        factorStatus(contractScore, 15),
                        contractDetail
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
