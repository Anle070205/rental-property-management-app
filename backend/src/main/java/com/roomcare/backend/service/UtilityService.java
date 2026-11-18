package com.roomcare.backend.service;

import com.roomcare.backend.dto.utility.CreateUtilityRequest;
import com.roomcare.backend.dto.utility.PreviousUtilityIndexResponse;
import com.roomcare.backend.dto.utility.UpdateUtilityRequest;
import com.roomcare.backend.dto.utility.UtilityRecordResponse;
import com.roomcare.backend.entity.*;
import com.roomcare.backend.repository.InvoiceRepository;
import com.roomcare.backend.repository.RoomRepository;
import com.roomcare.backend.repository.UserRepository;
import com.roomcare.backend.repository.UtilityRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UtilityService {

    private static final BigDecimal DEFAULT_ELECTRIC_PRICE = new BigDecimal("3500");
    private static final BigDecimal DEFAULT_WATER_PRICE = new BigDecimal("15000");
    private static final BigDecimal DEFAULT_INTERNET_FEE = new BigDecimal("100000");
    private static final BigDecimal DEFAULT_SERVICE_FEE = new BigDecimal("50000");
    private static final DateTimeFormatter LOG_TIME_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final UtilityRecordRepository utilityRecordRepository;
    private final RoomRepository roomRepository;
    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;

    @Transactional(readOnly = true)
    public List<UtilityRecordResponse> getUtilityRecords(
            Long landlordId,
            String month,
            Long roomId,
            Long propertyId) {

        String normalizedMonth = (month != null && !month.isBlank()) ? month.trim() : null;
        return utilityRecordRepository.findByLandlordWithFilters(landlordId, normalizedMonth, roomId, propertyId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PreviousUtilityIndexResponse getPreviousIndex(Long landlordId, Long roomId, String month) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));

        if (!room.getProperty().getLandlord().getId().equals(landlordId)) {
            throw new RuntimeException("Bạn không có quyền truy cập phòng này");
        }

        String targetMonth = (month != null && !month.isBlank()) ? month.trim() : "9999-12";
        Optional<UtilityRecord> prevOpt = utilityRecordRepository
                .findTopByRoomIdAndBillingMonthLessThanOrderByBillingMonthDesc(roomId, targetMonth);

        if (prevOpt.isEmpty()) {
            return new PreviousUtilityIndexResponse(
                    roomId,
                    null,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    DEFAULT_ELECTRIC_PRICE,
                    DEFAULT_WATER_PRICE,
                    DEFAULT_INTERNET_FEE,
                    DEFAULT_SERVICE_FEE
            );
        }

        UtilityRecord prev = prevOpt.get();
        return new PreviousUtilityIndexResponse(
                roomId,
                prev.getBillingMonth(),
                prev.getCurrElectricIndex(),
                prev.getCurrWaterIndex(),
                prev.getElectricUnitPrice(),
                prev.getWaterUnitPrice(),
                prev.getInternetFee(),
                prev.getServiceFee()
        );
    }

    @Transactional
    public UtilityRecordResponse createUtilityRecord(Long landlordId, CreateUtilityRequest request) {
        if (!request.getBillingMonth().matches("^\\d{4}-(0[1-9]|1[0-2])$")) {
            throw new RuntimeException("Kỳ ghi nhận phải theo định dạng YYYY-MM");
        }

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));

        if (!room.getProperty().getLandlord().getId().equals(landlordId)) {
            throw new RuntimeException("Bạn không có quyền ghi chỉ số cho phòng này");
        }

        if (utilityRecordRepository.existsByRoomIdAndBillingMonth(room.getId(), request.getBillingMonth())) {
            throw new RuntimeException("Phòng " + room.getRoomCode() + " đã có bản ghi điện nước kỳ " + request.getBillingMonth());
        }

        User landlord = userRepository.findById(landlordId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chủ nhà"));

        UtilityRecord record = new UtilityRecord();
        record.setLandlord(landlord);
        record.setProperty(room.getProperty());
        record.setRoom(room);
        record.setBillingMonth(request.getBillingMonth());

        applyReadingsAndCalculate(
                record,
                request.getPrevElectricIndex(),
                request.getCurrElectricIndex(),
                request.getElectricUnitPrice(),
                request.getPrevWaterIndex(),
                request.getCurrWaterIndex(),
                request.getWaterUnitPrice(),
                request.getInternetFee(),
                request.getServiceFee(),
                Boolean.TRUE.equals(request.getIsAdjustment()),
                request.getAdjustmentReason(),
                request.getNotes()
        );

        String logEntry = LocalDateTime.now().format(LOG_TIME_FORMAT)
                + " - Tạo mới: Điện " + formatIdx(record.getPrevElectricIndex()) + "→" + formatIdx(record.getCurrElectricIndex())
                + ", Nước " + formatIdx(record.getPrevWaterIndex()) + "→" + formatIdx(record.getCurrWaterIndex());
        record.setHistoryLog(logEntry);

        UtilityRecord saved = utilityRecordRepository.save(record);
        syncLinkedInvoiceIfPresent(saved);
        return mapToResponse(saved);
    }

    @Transactional
    public UtilityRecordResponse updateUtilityRecord(Long landlordId, Long recordId, UpdateUtilityRequest request) {
        UtilityRecord record = utilityRecordRepository.findById(recordId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy bản ghi điện nước"));

        if (!record.getLandlord().getId().equals(landlordId)) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa bản ghi này");
        }

        Optional<Invoice> linkedInvoice = invoiceRepository.findByRoomIdAndBillingMonth(
                record.getRoom().getId(), record.getBillingMonth()
        );
        if (linkedInvoice.isPresent() && linkedInvoice.get().getStatus() == InvoiceStatus.PAID) {
            throw new RuntimeException("Hóa đơn kỳ " + record.getBillingMonth() + " đã chốt thanh toán, không thể chỉnh sửa chỉ số");
        }

        BigDecimal oldElecUsage = record.getElectricUsage();
        BigDecimal oldWaterUsage = record.getWaterUsage();
        BigDecimal oldTotal = record.getTotalUtilityAmount();

        applyReadingsAndCalculate(
                record,
                request.getPrevElectricIndex(),
                request.getCurrElectricIndex(),
                request.getElectricUnitPrice(),
                request.getPrevWaterIndex(),
                request.getCurrWaterIndex(),
                request.getWaterUnitPrice(),
                request.getInternetFee(),
                request.getServiceFee(),
                Boolean.TRUE.equals(request.getIsAdjustment()),
                request.getAdjustmentReason(),
                request.getNotes()
        );

        String logEntry = LocalDateTime.now().format(LOG_TIME_FORMAT)
                + " - Cập nhật: Điện " + formatIdx(oldElecUsage) + "→" + formatIdx(record.getElectricUsage()) + " kWh"
                + ", Nước " + formatIdx(oldWaterUsage) + "→" + formatIdx(record.getWaterUsage()) + " m³"
                + " (Tổng " + oldTotal.setScale(0, RoundingMode.HALF_UP) + "→" + record.getTotalUtilityAmount().setScale(0, RoundingMode.HALF_UP) + "đ)";

        String existingLog = record.getHistoryLog();
        record.setHistoryLog(existingLog == null || existingLog.isBlank() ? logEntry : existingLog + "\n" + logEntry);

        UtilityRecord updated = utilityRecordRepository.save(record);
        syncLinkedInvoiceIfPresent(updated);
        return mapToResponse(updated);
    }

    private void applyReadingsAndCalculate(
            UtilityRecord record,
            BigDecimal prevElec,
            BigDecimal currElec,
            BigDecimal elecPrice,
            BigDecimal prevWater,
            BigDecimal currWater,
            BigDecimal waterPrice,
            BigDecimal internetFee,
            BigDecimal serviceFee,
            boolean isAdjustment,
            String adjustmentReason,
            String notes) {

        boolean elecDecreased = currElec.compareTo(prevElec) < 0;
        boolean waterDecreased = currWater.compareTo(prevWater) < 0;

        if ((elecDecreased || waterDecreased) && (!isAdjustment || adjustmentReason == null || adjustmentReason.isBlank())) {
            throw new RuntimeException("Chỉ số mới không được nhỏ hơn chỉ số cũ khi chưa bật chế độ điều chỉnh đồng hồ và nhập lý do");
        }

        BigDecimal elecUsage = elecDecreased ? currElec : currElec.subtract(prevElec);
        BigDecimal waterUsage = waterDecreased ? currWater : currWater.subtract(prevWater);

        BigDecimal safeInternet = internetFee != null ? internetFee : BigDecimal.ZERO;
        BigDecimal safeService = serviceFee != null ? serviceFee : BigDecimal.ZERO;

        BigDecimal electricityAmount = elecUsage.multiply(elecPrice).setScale(2, RoundingMode.HALF_UP);
        BigDecimal waterAmount = waterUsage.multiply(waterPrice).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalUtility = electricityAmount.add(waterAmount).add(safeInternet).add(safeService);

        record.setPrevElectricIndex(prevElec);
        record.setCurrElectricIndex(currElec);
        record.setElectricUsage(elecUsage);
        record.setElectricUnitPrice(elecPrice);
        record.setElectricityAmount(electricityAmount);

        record.setPrevWaterIndex(prevWater);
        record.setCurrWaterIndex(currWater);
        record.setWaterUsage(waterUsage);
        record.setWaterUnitPrice(waterPrice);
        record.setWaterAmount(waterAmount);

        record.setInternetFee(safeInternet);
        record.setServiceFee(safeService);
        record.setTotalUtilityAmount(totalUtility);
        record.setIsAdjustment(isAdjustment);
        record.setAdjustmentReason(adjustmentReason);
        record.setNotes(notes);
    }

    private void syncLinkedInvoiceIfPresent(UtilityRecord record) {
        Optional<Invoice> invoiceOpt = invoiceRepository.findByRoomIdAndBillingMonth(
                record.getRoom().getId(), record.getBillingMonth()
        );
        if (invoiceOpt.isEmpty()) {
            return;
        }

        Invoice invoice = invoiceOpt.get();
        if (invoice.getStatus() == InvoiceStatus.PAID) {
            throw new RuntimeException("Hóa đơn kỳ " + record.getBillingMonth() + " đã chốt thanh toán, không thể thay đổi số liệu");
        }

        invoice.setUtilityRecordId(record.getId());
        invoice.setElectricityAmount(record.getElectricityAmount());
        invoice.setWaterAmount(record.getWaterAmount());
        invoice.setInternetAmount(record.getInternetFee());
        invoice.setServiceAmount(record.getServiceFee());

        BigDecimal newTotal = invoice.getRoomRent()
                .add(record.getElectricityAmount())
                .add(record.getWaterAmount())
                .add(record.getInternetFee())
                .add(record.getServiceFee())
                .add(invoice.getOtherFees())
                .add(invoice.getPreviousDebt())
                .subtract(invoice.getDiscountAmount());

        if (newTotal.compareTo(invoice.getPaidAmount()) < 0) {
            throw new RuntimeException("Tổng hóa đơn sau cập nhật nhỏ hơn số tiền đã thanh toán trước đó");
        }

        BigDecimal remaining = newTotal.subtract(invoice.getPaidAmount());
        invoice.setTotalAmount(newTotal);
        invoice.setRemainingAmount(remaining);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            invoice.setStatus(InvoiceStatus.PAID);
        } else if (invoice.getDueDate().isBefore(LocalDate.now())) {
            invoice.setStatus(InvoiceStatus.OVERDUE);
        } else if (invoice.getPaidAmount().compareTo(BigDecimal.ZERO) > 0) {
            invoice.setStatus(InvoiceStatus.PARTIALLY_PAID);
        } else {
            invoice.setStatus(InvoiceStatus.UNPAID);
        }

        invoiceRepository.save(invoice);
    }

    private String formatIdx(BigDecimal val) {
        return val.stripTrailingZeros().toPlainString();
    }

    private UtilityRecordResponse mapToResponse(UtilityRecord record) {
        UtilityRecordResponse res = new UtilityRecordResponse();
        res.setId(record.getId());
        res.setPropertyId(record.getProperty().getId());
        res.setPropertyName(record.getProperty().getName());
        res.setRoomId(record.getRoom().getId());
        res.setRoomCode(record.getRoom().getRoomCode());
        res.setBillingMonth(record.getBillingMonth());
        res.setPrevElectricIndex(record.getPrevElectricIndex());
        res.setCurrElectricIndex(record.getCurrElectricIndex());
        res.setElectricUsage(record.getElectricUsage());
        res.setElectricUnitPrice(record.getElectricUnitPrice());
        res.setElectricityAmount(record.getElectricityAmount());
        res.setPrevWaterIndex(record.getPrevWaterIndex());
        res.setCurrWaterIndex(record.getCurrWaterIndex());
        res.setWaterUsage(record.getWaterUsage());
        res.setWaterUnitPrice(record.getWaterUnitPrice());
        res.setWaterAmount(record.getWaterAmount());
        res.setInternetFee(record.getInternetFee());
        res.setServiceFee(record.getServiceFee());
        res.setTotalUtilityAmount(record.getTotalUtilityAmount());
        res.setAdjustment(Boolean.TRUE.equals(record.getIsAdjustment()));
        res.setAdjustmentReason(record.getAdjustmentReason());
        res.setNotes(record.getNotes());
        res.setHistoryLog(record.getHistoryLog());

        Optional<Invoice> linkedInvoice = invoiceRepository.findByRoomIdAndBillingMonth(
                record.getRoom().getId(), record.getBillingMonth()
        );
        if (linkedInvoice.isPresent()) {
            Invoice inv = linkedInvoice.get();
            res.setLinkedInvoiceId(inv.getId());
            res.setLinkedInvoiceCode(inv.getInvoiceCode());
            res.setInvoiceLocked(inv.getStatus() == InvoiceStatus.PAID);
        } else {
            res.setInvoiceLocked(false);
        }

        res.setCreatedAt(record.getCreatedAt());
        res.setUpdatedAt(record.getUpdatedAt());
        return res;
    }
}
