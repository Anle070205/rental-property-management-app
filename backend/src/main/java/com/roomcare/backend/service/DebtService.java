package com.roomcare.backend.service;

import com.roomcare.backend.dto.debt.DebtHistoryItemResponse;
import com.roomcare.backend.dto.debt.DebtSummaryResponse;
import com.roomcare.backend.dto.debt.RoomTenantDebtResponse;
import com.roomcare.backend.dto.invoice.InvoiceResponse;
import com.roomcare.backend.entity.Invoice;
import com.roomcare.backend.entity.InvoiceStatus;
import com.roomcare.backend.entity.Payment;
import com.roomcare.backend.entity.Tenant;
import com.roomcare.backend.repository.InvoiceRepository;
import com.roomcare.backend.repository.PaymentRepository;
import com.roomcare.backend.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DebtService {

    private static final DateTimeFormatter DT_DISPLAY = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final TenantRepository tenantRepository;

    @Transactional(readOnly = true)
    public DebtSummaryResponse getDebts(
            Long landlordId,
            Long roomId,
            Long tenantId,
            String month,
            String overdueLevel,
            String keyword) {

        String normalizedMonth = (month != null && !month.isBlank()) ? month.trim() : null;
        String normalizedKeyword = (keyword != null && !keyword.isBlank()) ? keyword.trim().toLowerCase() : null;
        String normalizedLevel = (overdueLevel != null && !overdueLevel.isBlank()) ? overdueLevel.trim().toUpperCase() : null;

        List<Invoice> allInvoices = invoiceRepository.findAllByLandlordWithDetails(landlordId);

        List<Invoice> filteredInvoices = allInvoices.stream()
                .filter(inv -> roomId == null || inv.getRoom().getId().equals(roomId))
                .filter(inv -> tenantId == null || (inv.getTenant() != null && inv.getTenant().getId().equals(tenantId)))
                .filter(inv -> normalizedMonth == null || inv.getBillingMonth().equals(normalizedMonth))
                .collect(Collectors.toList());

        Map<String, List<Invoice>> grouped = new LinkedHashMap<>();
        for (Invoice inv : filteredInvoices) {
            String key = inv.getRoom().getId() + "_" + (inv.getTenant() != null ? inv.getTenant().getId() : 0L);
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(inv);
        }

        List<RoomTenantDebtResponse> items = new ArrayList<>();
        for (List<Invoice> groupInvoices : grouped.values()) {
            RoomTenantDebtResponse item = buildDebtGroup(groupInvoices);
            if (item.getTotalDebt().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            if (normalizedLevel != null && !normalizedLevel.equals(item.getOverdueLevel())) {
                continue;
            }
            if (normalizedKeyword != null && !matchesKeyword(item, normalizedKeyword)) {
                continue;
            }
            items.add(item);
        }

        items.sort(Comparator
                .comparingLong(RoomTenantDebtResponse::getMaxDaysOverdue).reversed()
                .thenComparing(RoomTenantDebtResponse::getTotalDebt, Comparator.reverseOrder()));

        BigDecimal totalDebtAmount = items.stream()
                .map(RoomTenantDebtResponse::getTotalDebt)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int totalUnpaidInvoices = items.stream().mapToInt(RoomTenantDebtResponse::getUnpaidInvoicesCount).sum();
        int totalOverdueInvoices = items.stream().mapToInt(RoomTenantDebtResponse::getOverdueInvoicesCount).sum();

        return new DebtSummaryResponse(
                totalDebtAmount,
                totalUnpaidInvoices,
                totalOverdueInvoices,
                items.size(),
                items
        );
    }

    @Transactional(readOnly = true)
    public RoomTenantDebtResponse getTenantDebtDetail(Long landlordId, Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người thuê"));

        if (!tenant.getLandlord().getId().equals(landlordId)) {
            throw new RuntimeException("Bạn không có quyền xem công nợ của người thuê này");
        }

        List<Invoice> invoices = invoiceRepository.findByTenantIdAndLandlordIdWithDetails(tenantId, landlordId);
        if (invoices.isEmpty()) {
            RoomTenantDebtResponse empty = new RoomTenantDebtResponse();
            empty.setTenantId(tenant.getId());
            empty.setTenantName(tenant.getFullName());
            empty.setTenantPhone(tenant.getPhone());
            empty.setPropertyId(tenant.getProperty().getId());
            empty.setPropertyName(tenant.getProperty().getName());
            empty.setTotalBilled(BigDecimal.ZERO);
            empty.setTotalPaid(BigDecimal.ZERO);
            empty.setTotalDebt(BigDecimal.ZERO);
            empty.setOverdueLevel("NOT_DUE");
            return empty;
        }

        return buildDebtGroup(invoices);
    }

    private RoomTenantDebtResponse buildDebtGroup(List<Invoice> invoices) {
        Invoice sample = invoices.get(0);
        LocalDate today = LocalDate.now();

        BigDecimal totalBilled = BigDecimal.ZERO;
        BigDecimal totalPaid = BigDecimal.ZERO;
        BigDecimal totalDebt = BigDecimal.ZERO;
        int overdueCount = 0;
        long maxDaysOverdue = 0L;

        List<InvoiceResponse> unpaidInvoices = new ArrayList<>();
        List<DebtEvent> rawEvents = new ArrayList<>();

        for (Invoice inv : invoices) {
            totalBilled = totalBilled.add(inv.getTotalAmount());
            totalPaid = totalPaid.add(inv.getPaidAmount());
            totalDebt = totalDebt.add(inv.getRemainingAmount());

            long daysOverdue = 0;
            if (inv.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0 && inv.getDueDate().isBefore(today)) {
                daysOverdue = ChronoUnit.DAYS.between(inv.getDueDate(), today);
                overdueCount++;
                if (daysOverdue > maxDaysOverdue) {
                    maxDaysOverdue = daysOverdue;
                }
            }

            if (inv.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0) {
                unpaidInvoices.add(toInvoiceResponse(inv, daysOverdue));
            }

            LocalDateTime invTime = inv.getCreatedAt() != null
                    ? inv.getCreatedAt()
                    : LocalDate.parse(inv.getBillingMonth() + "-01").atStartOfDay();
            rawEvents.add(new DebtEvent(
                    "INVOICE",
                    inv.getInvoiceCode(),
                    inv.getBillingMonth(),
                    inv.getTotalAmount(),
                    invTime,
                    "Phát sinh hóa đơn kỳ " + inv.getBillingMonth()
            ));

            List<Payment> payments = paymentRepository.findByInvoiceIdOrderByPaidAtDesc(inv.getId());
            for (Payment p : payments) {
                String methodLabel = "BANK_TRANSFER".equals(p.getPaymentMethod().name()) ? "Chuyển khoản" : "Tiền mặt";
                String code = p.getTransactionCode() != null ? " (" + p.getTransactionCode() + ")" : "";
                rawEvents.add(new DebtEvent(
                        "PAYMENT",
                        p.getTransactionCode() != null ? p.getTransactionCode() : inv.getInvoiceCode(),
                        inv.getBillingMonth(),
                        p.getAmount().negate(),
                        p.getPaidAt(),
                        "Thu " + methodLabel + code + " cho kỳ " + inv.getBillingMonth()
                ));
            }
        }

        unpaidInvoices.sort(Comparator.comparing(InvoiceResponse::getDueDate));

        rawEvents.sort(Comparator.comparing(e -> e.occurredAt));
        BigDecimal runningBalance = BigDecimal.ZERO;
        List<DebtHistoryItemResponse> history = new ArrayList<>();
        for (DebtEvent ev : rawEvents) {
            runningBalance = runningBalance.add(ev.delta);
            history.add(new DebtHistoryItemResponse(
                    ev.type,
                    ev.referenceCode,
                    ev.billingMonth,
                    ev.delta,
                    runningBalance,
                    ev.occurredAt.format(DT_DISPLAY),
                    ev.description
            ));
        }
        Collections.reverse(history);

        RoomTenantDebtResponse response = new RoomTenantDebtResponse();
        response.setRoomId(sample.getRoom().getId());
        response.setRoomCode(sample.getRoom().getRoomCode());
        response.setPropertyId(sample.getProperty().getId());
        response.setPropertyName(sample.getProperty().getName());
        if (sample.getTenant() != null) {
            response.setTenantId(sample.getTenant().getId());
            response.setTenantName(sample.getTenant().getFullName());
            response.setTenantPhone(sample.getTenant().getPhone());
        }
        response.setTotalBilled(totalBilled);
        response.setTotalPaid(totalPaid);
        response.setTotalDebt(totalDebt);
        response.setUnpaidInvoicesCount(unpaidInvoices.size());
        response.setOverdueInvoicesCount(overdueCount);
        response.setMaxDaysOverdue(maxDaysOverdue);
        response.setOverdueLevel(resolveOverdueLevel(maxDaysOverdue));
        response.setLatestInvoiceId(!unpaidInvoices.isEmpty() ? unpaidInvoices.get(0).getId() : sample.getId());
        response.setUnpaidInvoices(unpaidInvoices);
        response.setDebtHistory(history);
        return response;
    }

    private String resolveOverdueLevel(long maxDaysOverdue) {
        if (maxDaysOverdue <= 0) {
            return "NOT_DUE";
        }
        if (maxDaysOverdue <= 7) {
            return "OVERDUE_1_7";
        }
        if (maxDaysOverdue <= 30) {
            return "OVERDUE_8_30";
        }
        return "OVERDUE_30_PLUS";
    }

    private boolean matchesKeyword(RoomTenantDebtResponse item, String keyword) {
        return (item.getRoomCode() != null && item.getRoomCode().toLowerCase().contains(keyword))
                || (item.getPropertyName() != null && item.getPropertyName().toLowerCase().contains(keyword))
                || (item.getTenantName() != null && item.getTenantName().toLowerCase().contains(keyword))
                || (item.getTenantPhone() != null && item.getTenantPhone().toLowerCase().contains(keyword));
    }

    private InvoiceResponse toInvoiceResponse(Invoice inv, long daysOverdue) {
        InvoiceResponse res = new InvoiceResponse();
        res.setId(inv.getId());
        res.setInvoiceCode(inv.getInvoiceCode());
        res.setPropertyId(inv.getProperty().getId());
        res.setPropertyName(inv.getProperty().getName());
        res.setRoomId(inv.getRoom().getId());
        res.setRoomCode(inv.getRoom().getRoomCode());
        if (inv.getTenant() != null) {
            res.setTenantId(inv.getTenant().getId());
            res.setTenantName(inv.getTenant().getFullName());
            res.setTenantPhone(inv.getTenant().getPhone());
        }
        res.setBillingMonth(inv.getBillingMonth());
        res.setRoomRent(inv.getRoomRent());
        res.setElectricityAmount(inv.getElectricityAmount());
        res.setWaterAmount(inv.getWaterAmount());
        res.setInternetAmount(inv.getInternetAmount());
        res.setServiceAmount(inv.getServiceAmount());
        res.setOtherFees(inv.getOtherFees());
        res.setPreviousDebt(inv.getPreviousDebt());
        res.setDiscountAmount(inv.getDiscountAmount());
        res.setTotalAmount(inv.getTotalAmount());
        res.setPaidAmount(inv.getPaidAmount());
        res.setRemainingAmount(inv.getRemainingAmount());
        res.setStatus(daysOverdue > 0 && inv.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0
                ? InvoiceStatus.OVERDUE
                : inv.getStatus());
        res.setDueDate(inv.getDueDate());
        res.setDaysOverdue(daysOverdue);
        res.setNotes(inv.getNotes());
        return res;
    }

    private record DebtEvent(
            String type,
            String referenceCode,
            String billingMonth,
            BigDecimal delta,
            LocalDateTime occurredAt,
            String description
    ) {}
}
