package com.roomcare.backend.service;

import com.roomcare.backend.dto.roommate.RoommateResponse;
import com.roomcare.backend.dto.tenant.MyContractResponse;
import com.roomcare.backend.dto.tenant.MyRoomResponse;
import com.roomcare.backend.dto.tenant.SubmitPaymentProofRequest;
import com.roomcare.backend.dto.tenant.TenantInvoiceResponse;
import com.roomcare.backend.dto.tenant.TenantPaymentInfoResponse;
import com.roomcare.backend.dto.tenant.TenantPaymentResponse;
import com.roomcare.backend.entity.BookingRequest;
import com.roomcare.backend.entity.Contract;
import com.roomcare.backend.entity.ContractStatus;
import com.roomcare.backend.entity.Invoice;
import com.roomcare.backend.entity.InvoiceStatus;
import com.roomcare.backend.entity.NotificationType;
import com.roomcare.backend.entity.Payment;
import com.roomcare.backend.entity.Room;
import com.roomcare.backend.entity.RoomStatus;
import com.roomcare.backend.entity.Roommate;
import com.roomcare.backend.entity.Tenant;
import com.roomcare.backend.repository.BookingRequestRepository;
import com.roomcare.backend.repository.ContractRepository;
import com.roomcare.backend.repository.InvoiceRepository;
import com.roomcare.backend.repository.PaymentRepository;
import com.roomcare.backend.repository.RoomRepository;
import com.roomcare.backend.repository.RoommateRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
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
    private final BookingRequestRepository bookingRequestRepository;
    private final NotificationService notificationService;

    @Autowired(required = false)
    private ContractRepository contractRepository;

    @Autowired(required = false)
    private RoommateRepository roommateRepository;

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
            int healthScore = room.getHealthScore() != null ? room.getHealthScore() : 100;
            return new MyRoomResponse(
                    room.getRoomCode(),
                    fullAddress,
                    room.getRentPrice(),
                    amenities,
                    tenant.getLandlord().getFullName(),
                    tenant.getLandlord().getPhone(),
                    healthScore,
                    room.getId(),
                    room.getProperty().getId(),
                    room.getFloor(),
                    room.getArea(),
                    room.getDeposit(),
                    room.getElectricityPrice(),
                    room.getWaterPrice(),
                    room.getInternetFee(),
                    room.getServiceFee()
            );
        });
    }

    @Override
    public Optional<MyContractResponse> contract(Tenant tenant) {
        Optional<Contract> contractOpt = findPrimaryContractForTenant(tenant);
        if (contractOpt.isPresent()) {
            Contract c = contractOpt.get();
            List<RoommateResponse> roommates = roommateRepository != null
                    ? roommateRepository.findByContractIdOrderByCreatedAtAsc(c.getId())
                    .stream().map(RoommateService::toResponse).collect(Collectors.toList())
                    : List.of();
            return Optional.of(new MyContractResponse(
                    c.getId(),
                    c.getStatus().name(),
                    c.getStartDate(),
                    c.getEndDate(),
                    c.getMonthlyRent(),
                    c.getDepositAmount(),
                    c.getTerms(),
                    c.getContractCode(),
                    c.getRoom().getRoomCode(),
                    c.getProperty().getName(),
                    c.getProperty().getAddress(),
                    c.getLandlord().getFullName(),
                    c.getLandlord().getPhone(),
                    c.getElectricityUnitPrice(),
                    c.getWaterUnitPrice(),
                    c.getWaterBillingMode() != null ? c.getWaterBillingMode().name() : "METERED",
                    c.getInternetFee(),
                    c.getGarbageFee(),
                    c.getServiceFee(),
                    c.getPaymentDayOfMonth(),
                    c.getDocumentUrl(),
                    c.getSignedAt(),
                    roommates
            ));
        }

        return findActiveRoom(tenant).map(room -> {
            LocalDate startDate = tenant.getCreatedAt() != null
                    ? tenant.getCreatedAt().toLocalDate().withDayOfMonth(1)
                    : LocalDate.now().withDayOfMonth(1);
            LocalDate endDate = startDate.plusYears(1).minusDays(1);
            if (!endDate.isAfter(LocalDate.now())) {
                endDate = LocalDate.now().plusMonths(3);
            }
            String terms = "Hợp đồng thuê phòng " + room.getRoomCode() + " tại " + room.getProperty().getName()
                    + ". Thanh toán tiền phòng và điện nước định kỳ hàng tháng trước ngày hạn thanh toán. "
                    + "Giữ gìn vệ sinh chung, tài sản phòng và tuân thủ nội quy khu trọ.";
            return new MyContractResponse(
                    room.getId(),
                    "ACTIVE",
                    startDate,
                    endDate,
                    room.getRentPrice(),
                    room.getDeposit(),
                    terms,
                    null,
                    room.getRoomCode(),
                    room.getProperty().getName(),
                    room.getProperty().getAddress(),
                    tenant.getLandlord().getFullName(),
                    tenant.getLandlord().getPhone(),
                    room.getElectricityPrice(),
                    room.getWaterPrice(),
                    "METERED",
                    room.getInternetFee(),
                    BigDecimal.ZERO,
                    room.getServiceFee(),
                    5,
                    null,
                    null,
                    List.of()
            );
        });
    }

    @Override
    public boolean requestRenewal(Tenant tenant, Long contractId) {
        if (contractId == null) {
            return false;
        }
        Optional<Contract> contractOpt = findPrimaryContractForTenant(tenant);
        if (contractOpt.isPresent()) {
            Contract contract = contractOpt.get();
            if (!Objects.equals(contract.getId(), contractId)
                    && !Objects.equals(contract.getRoom().getId(), contractId)) {
                return false;
            }
            notificationService.createNotification(
                    tenant.getLandlord(),
                    NotificationType.CONTRACT,
                    "Yêu cầu gia hạn hợp đồng",
                    "Người thuê " + tenant.getFullName() + " (HĐ " + contract.getContractCode()
                            + " - Phòng " + contract.getRoom().getRoomCode() + ") đã gửi yêu cầu gia hạn hợp đồng.",
                    contract.getId()
            );
            return true;
        }

        Optional<Room> roomOpt = findActiveRoom(tenant);
        if (roomOpt.isEmpty()) {
            return false;
        }
        Room room = roomOpt.get();
        if (!contractId.equals(room.getId()) && !contractId.equals(tenant.getId())) {
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
                .filter(inv -> isVisibleToTenant(inv))
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
                .filter(this::isVisibleToTenant)
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
                .filter(this::isVisibleToTenant)
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
                .findByTenantIdAndRemainingAmountGreaterThanOrderByDueDateAsc(tenant.getId(), BigDecimal.ZERO)
                .stream()
                .filter(this::isVisibleToTenant)
                .collect(Collectors.toList());
        if (unpaid.isEmpty()) {
            return Optional.empty();
        }
        BigDecimal totalRemaining = unpaid.stream()
                .map(Invoice::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        LocalDate earliestDue = unpaid.get(0).getDueDate();
        return Optional.of(new AmountDue(totalRemaining, earliestDue));
    }

    private Optional<Contract> findPrimaryContractForTenant(Tenant tenant) {
        if (contractRepository == null || tenant == null || tenant.getId() == null) {
            return Optional.empty();
        }
        Optional<Contract> activeOrSigned = contractRepository
                .findFirstByTenantIdAndStatusInOrderByActivatedAtDescCreatedAtDesc(
                        tenant.getId(), List.of(ContractStatus.ACTIVE, ContractStatus.SIGNED));
        if (activeOrSigned.isPresent()) {
            return activeOrSigned;
        }
        return contractRepository.findByTenantIdOrderByStartDateDescCreatedAtDesc(tenant.getId())
                .stream()
                .filter(c -> c.getStatus() != ContractStatus.DRAFT)
                .findFirst();
    }

    private Optional<Room> findActiveRoom(Tenant tenant) {
        if (contractRepository != null && tenant != null && tenant.getId() != null) {
            Optional<Contract> activeContract = contractRepository
                    .findFirstByTenantIdAndStatusInOrderByActivatedAtDescCreatedAtDesc(
                            tenant.getId(), List.of(ContractStatus.ACTIVE, ContractStatus.SIGNED));
            if (activeContract.isPresent()) {
                return Optional.of(activeContract.get().getRoom());
            }
        }
        if (bookingRequestRepository != null && tenant.getUser() != null) {
            Optional<BookingRequest> approvedBooking = bookingRequestRepository
                    .findFirstByTenantUserIdAndRequestTypeAndStatusOrderByUpdatedAtDesc(
                            tenant.getUser().getId(),
                            BookingRequest.RequestType.BOOKING,
                            BookingRequest.Status.APPROVED
                    );
            if (approvedBooking.isPresent()) {
                return Optional.of(approvedBooking.get().getRoom());
            }
        }
        List<Invoice> tenantInvoices = invoiceRepository
                .findByTenantIdOrderByBillingMonthDescCreatedAtDesc(tenant.getId())
                .stream()
                .filter(this::isVisibleToTenant)
                .collect(Collectors.toList());
        if (!tenantInvoices.isEmpty()) {
            return Optional.of(tenantInvoices.get(0).getRoom());
        }
        // Tuyet doi khong tra phong cua nguoi khac trong cung nha tro
        return Optional.empty();
    }

    private boolean isVisibleToTenant(Invoice invoice) {
        if (invoice == null || invoice.getStatus() == null) {
            return false;
        }
        String name = invoice.getStatus().name();
        return !"DRAFT".equals(name) && !"CANCELLED".equals(name);
    }

    private TenantInvoiceResponse mapInvoice(Invoice invoice) {
        String effectiveStatus = invoice.getStatus().name();
        if (invoice.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0
                && invoice.getDueDate() != null
                && invoice.getDueDate().isBefore(LocalDate.now())
                && isVisibleToTenant(invoice)) {
            effectiveStatus = InvoiceStatus.OVERDUE.name();
        }
        List<TenantPaymentResponse> payments = invoice.getId() != null
                ? paymentRepository.findByInvoiceIdOrderByPaidAtDesc(invoice.getId())
                        .stream()
                        .map(p -> new TenantPaymentResponse(
                                p.getId(),
                                invoice.getId(),
                                p.getPaidAt(),
                                p.getAmount(),
                                p.getPaymentMethod().name(),
                                "CONFIRMED"
                        ))
                        .collect(Collectors.toList())
                : List.of();
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
                invoice.getRemainingAmount(),
                invoice.getProperty() != null ? invoice.getProperty().getName() : null,
                invoice.getRoom() != null ? invoice.getRoom().getRoomCode() : null,
                invoice.getTenant() != null ? invoice.getTenant().getFullName() : null,
                invoice.getContract() != null ? invoice.getContract().getContractCode() : null,
                invoice.getPrevElectricIndex(),
                invoice.getCurrElectricIndex(),
                invoice.getElectricUsage(),
                invoice.getElectricUnitPrice(),
                invoice.getPrevWaterIndex(),
                invoice.getCurrWaterIndex(),
                invoice.getWaterUsage(),
                invoice.getWaterUnitPrice(),
                invoice.getWaterBillingMode() != null ? invoice.getWaterBillingMode().name() : "METERED",
                invoice.getOccupantCount(),
                invoice.getGarbageFee(),
                invoice.getAdjustmentAmount(),
                invoice.getAdjustmentReason(),
                invoice.getNotes(),
                invoice.getPublishedAt(),
                invoice.getPaymentProofNote(),
                invoice.getPaymentProofUrl(),
                invoice.getPaymentProofSubmittedAt(),
                payments
        );
    }

    @Override
    public Optional<TenantInvoiceResponse> submitPaymentProof(
            Tenant tenant,
            Long invoiceId,
            SubmitPaymentProofRequest request) {
        if (invoiceId == null || request == null) {
            return Optional.empty();
        }
        Optional<Invoice> invoiceOpt = invoiceRepository.findById(invoiceId)
                .filter(inv -> inv.getTenant() != null && inv.getTenant().getId().equals(tenant.getId()))
                .filter(this::isVisibleToTenant);
        if (invoiceOpt.isEmpty()) {
            return Optional.empty();
        }
        Invoice invoice = invoiceOpt.get();
        if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getRemainingAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Hóa đơn này đã được thanh toán đủ");
        }
        String note = request.note() != null ? request.note().trim() : "";
        String url = request.proofUrl() != null ? request.proofUrl().trim() : "";
        if (note.isEmpty() && url.isEmpty()) {
            throw new RuntimeException("Vui lòng nhập nội dung giao dịch hoặc liên kết ảnh chứng từ chuyển khoản");
        }
        invoice.setPaymentProofNote(note.isEmpty() ? null : note);
        invoice.setPaymentProofUrl(url.isEmpty() ? null : url);
        invoice.setPaymentProofSubmittedAt(java.time.LocalDateTime.now());
        // Không tự động đổi trạng thái thành PAID - chỉ chủ trọ xác nhận thực thu mới chốt thanh toán
        Invoice saved = invoiceRepository.save(invoice);

        if (notificationService != null && saved.getLandlord() != null) {
            notificationService.createNotification(
                    saved.getLandlord(),
                    NotificationType.PAYMENT,
                    "Khách gửi minh chứng thanh toán",
                    "Khách thuê " + tenant.getFullName() + " đã gửi minh chứng cho hóa đơn "
                            + saved.getInvoiceCode() + " (Phòng "
                            + (saved.getRoom() != null ? saved.getRoom().getRoomCode() : "")
                            + "). Vui lòng kiểm tra và xác nhận thực thu.",
                    saved.getId()
            );
        }
        return Optional.of(mapInvoice(saved));
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
