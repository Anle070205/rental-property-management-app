package com.roomcare.backend.service;

import com.roomcare.backend.dto.tenant.MyContractResponse;
import com.roomcare.backend.dto.tenant.MyRoomResponse;
import com.roomcare.backend.dto.tenant.TenantInvoiceResponse;
import com.roomcare.backend.dto.tenant.TenantPaymentInfoResponse;
import com.roomcare.backend.dto.tenant.TenantPaymentResponse;
import com.roomcare.backend.entity.Invoice;
import com.roomcare.backend.entity.InvoiceStatus;
import com.roomcare.backend.entity.NotificationType;
import com.roomcare.backend.entity.Payment;
import com.roomcare.backend.entity.Room;
import com.roomcare.backend.entity.RoomStatus;
import com.roomcare.backend.entity.Tenant;
import com.roomcare.backend.repository.InvoiceRepository;
import com.roomcare.backend.repository.PaymentRepository;
import com.roomcare.backend.repository.RoomRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class TenantPortalProviderImpl implements TenantPortalProvider {

    private static final String DEFAULT_BANK_NAME = "Vietcombank (CN TP.HCM)";
    private static final String DEFAULT_BANK_ACCOUNT = "1029384756";

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final RoomRepository roomRepository;
    private final NotificationService notificationService;

    @Override
    public Optional<MyRoomResponse> room(Tenant tenant) {
        return findActiveRoom(tenant).map(room -> {
            List<String> amenities = room.getAmenities() == null || room.getAmenities().isBlank()
                    ? List.of()
                    : Arrays.stream(room.getAmenities().split(","))
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .collect(Collectors.toList());
            String fullAddress = room.getProperty().getName() + " - " + room.getProperty().getAddress();
            return new MyRoomResponse(
                    room.getRoomCode(),
                    fullAddress,
                    room.getRentPrice(),
                    amenities,
                    tenant.getLandlord().getFullName(),
                    tenant.getLandlord().getPhone()
            );
        });
    }

    @Override
    public Optional<MyContractResponse> contract(Tenant tenant) {
        return findActiveRoom(tenant).map(room -> {
            LocalDate startDate = tenant.getCreatedAt() != null
                    ? tenant.getCreatedAt().toLocalDate().withDayOfMonth(1)
                    : LocalDate.now().minusMonths(6).withDayOfMonth(1);
            LocalDate endDate = startDate.plusYears(1).minusDays(1);
            if (!endDate.isAfter(LocalDate.now())) {
                endDate = LocalDate.now().plusMonths(3);
            }
            String status = room.getStatus() == RoomStatus.EXPIRING_SOON ? "ACTIVE" : "ACTIVE";
            String terms = "Hợp đồng thuê phòng " + room.getRoomCode() + " tại " + room.getProperty().getName()
                    + ". Thanh toán tiền phòng và điện nước định kỳ hàng tháng trước ngày hạn thanh toán. "
                    + "Giữ gìn vệ sinh chung, tài sản phòng và tuân thủ nội quy khu trọ.";
            return new MyContractResponse(
                    room.getId(),
                    status,
                    startDate,
                    endDate,
                    room.getRentPrice(),
                    room.getDeposit(),
                    terms
            );
        });
    }

    @Override
    public boolean requestRenewal(Tenant tenant, Long contractId) {
        Optional<Room> roomOpt = findActiveRoom(tenant);
        if (roomOpt.isEmpty()) {
            return false;
        }
        Room room = roomOpt.get();
        if (contractId == null || (!contractId.equals(room.getId()) && !contractId.equals(tenant.getId()))) {
            return false;
        }
        notificationService.createNotification(
                tenant.getLandlord(),
                NotificationType.CONTRACT,
                "Yêu cầu gia hạn hợp đồng",
                "Người thuê " + tenant.getFullName() + " (Phòng " + room.getRoomCode()
                        + ") đã gửi yêu cầu gia hạn hợp đồng.",
                room.getId()
        );
        return true;
    }

    @Override
    public Page<TenantInvoiceResponse> invoices(Tenant tenant, String month, String status, Pageable pageable) {
        String normalizedMonth = month != null && !month.isBlank() ? month.trim() : null;
        String normalizedStatus = status != null && !status.isBlank() ? status.trim().toUpperCase() : null;
        List<TenantInvoiceResponse> filtered = invoiceRepository
                .findByTenantIdOrderByBillingMonthDescCreatedAtDesc(tenant.getId())
                .stream()
                .map(this::mapInvoice)
                .filter(inv -> normalizedMonth == null || inv.billingMonth().equals(normalizedMonth))
                .filter(inv -> normalizedStatus == null || inv.status().equals(normalizedStatus))
                .collect(Collectors.toList());
        return paginate(filtered, pageable);
    }

    @Override
    public Optional<TenantInvoiceResponse> invoice(Tenant tenant, Long invoiceId) {
        if (invoiceId == null) {
            return Optional.empty();
        }
        return invoiceRepository.findById(invoiceId)
                .filter(inv -> inv.getTenant() != null && inv.getTenant().getId().equals(tenant.getId()))
                .map(this::mapInvoice);
    }

    @Override
    public Optional<TenantPaymentInfoResponse> paymentInfo(Tenant tenant, Long invoiceId) {
        return invoice(tenant, invoiceId)
                .filter(inv -> inv.remainingAmount().compareTo(BigDecimal.ZERO) > 0)
                .map(inv -> {
                    String transferContent = "ROOMCARE " + inv.invoiceCode();
                    String payload = "ROOMCARE|BANK:VCB|ACC:" + DEFAULT_BANK_ACCOUNT
                            + "|AMT:" + inv.remainingAmount().toPlainString()
                            + "|MSG:" + transferContent;
                    return new TenantPaymentInfoResponse(
                            DEFAULT_BANK_NAME,
                            DEFAULT_BANK_ACCOUNT,
                            inv.remainingAmount(),
                            transferContent,
                            payload
                    );
                });
    }

    @Override
    public Page<TenantPaymentResponse> payments(Tenant tenant, Pageable pageable) {
        List<Long> invoiceIds = invoiceRepository.findByTenantIdOrderByBillingMonthDescCreatedAtDesc(tenant.getId())
                .stream()
                .map(Invoice::getId)
                .collect(Collectors.toList());
        if (invoiceIds.isEmpty()) {
            return Page.empty(pageable);
        }
        Page<Payment> page = paymentRepository.findByInvoiceIdInOrderByPaidAtDesc(invoiceIds, pageable);
        List<TenantPaymentResponse> items = page.getContent().stream()
                .map(p -> new TenantPaymentResponse(
                        p.getId(),
                        p.getInvoice().getId(),
                        p.getPaidAt(),
                        p.getAmount(),
                        p.getPaymentMethod().name(),
                        "CONFIRMED"
                ))
                .collect(Collectors.toList());
        return new PageImpl<>(items, pageable, page.getTotalElements());
    }

    @Override
    public Optional<AmountDue> amountDue(Tenant tenant) {
        List<Invoice> unpaid = invoiceRepository
                .findByTenantIdAndRemainingAmountGreaterThanOrderByDueDateAsc(tenant.getId(), BigDecimal.ZERO);
        if (unpaid.isEmpty()) {
            return Optional.empty();
        }
        BigDecimal totalRemaining = unpaid.stream()
                .map(Invoice::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDate earliestDue = unpaid.get(0).getDueDate();
        return Optional.of(new AmountDue(totalRemaining, earliestDue));
    }

    private Optional<Room> findActiveRoom(Tenant tenant) {
        List<Invoice> tenantInvoices = invoiceRepository
                .findByTenantIdOrderByBillingMonthDescCreatedAtDesc(tenant.getId());
        if (!tenantInvoices.isEmpty()) {
            return Optional.of(tenantInvoices.get(0).getRoom());
        }
        if (tenant.getProperty() != null) {
            List<Room> occupiedRooms = roomRepository.findByPropertyIdAndStatusInOrderByRoomCodeAsc(
                    tenant.getProperty().getId(),
                    List.of(RoomStatus.OCCUPIED, RoomStatus.EXPIRING_SOON, RoomStatus.NEEDS_ATTENTION)
            );
            if (!occupiedRooms.isEmpty()) {
                return Optional.of(occupiedRooms.get(0));
            }
        }
        return Optional.empty();
    }

    private TenantInvoiceResponse mapInvoice(Invoice invoice) {
        String effectiveStatus = invoice.getStatus().name();
        if (invoice.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0
                && invoice.getDueDate() != null
                && invoice.getDueDate().isBefore(LocalDate.now())) {
            effectiveStatus = InvoiceStatus.OVERDUE.name();
        }
        return new TenantInvoiceResponse(
                invoice.getId(),
                invoice.getInvoiceCode(),
                invoice.getBillingMonth(),
                effectiveStatus,
                invoice.getDueDate(),
                invoice.getRoomRent(),
                invoice.getElectricityAmount(),
                invoice.getWaterAmount(),
                invoice.getInternetAmount(),
                invoice.getServiceAmount(),
                invoice.getOtherFees(),
                invoice.getPreviousDebt(),
                invoice.getDiscountAmount(),
                invoice.getTotalAmount(),
                invoice.getPaidAmount(),
                invoice.getRemainingAmount()
        );
    }

    private <T> Page<T> paginate(List<T> items, Pageable pageable) {
        int start = (int) pageable.getOffset();
        if (start >= items.size()) {
            return new PageImpl<>(List.of(), pageable, items.size());
        }
        int end = Math.min(start + pageable.getPageSize(), items.size());
        return new PageImpl<>(items.subList(start, end), pageable, items.size());
    }
}
