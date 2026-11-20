package com.roomcare.backend.service;

import com.roomcare.backend.dto.invoice.AdjustInvoiceRequest;
import com.roomcare.backend.dto.invoice.BatchCreateInvoiceRequest;
import com.roomcare.backend.dto.invoice.BatchCreateInvoiceResult;
import com.roomcare.backend.dto.invoice.CancelInvoiceRequest;
import com.roomcare.backend.dto.invoice.CreateInvoiceRequest;
import com.roomcare.backend.dto.invoice.CreatePaymentRequest;
import com.roomcare.backend.dto.invoice.InvoiceResponse;
import com.roomcare.backend.dto.invoice.PaymentResponse;
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
import com.roomcare.backend.entity.User;
import com.roomcare.backend.entity.UtilityRecord;
import com.roomcare.backend.entity.WaterBillingMode;
import com.roomcare.backend.repository.ContractRepository;
import com.roomcare.backend.repository.InvoiceRepository;
import com.roomcare.backend.repository.PaymentRepository;
import com.roomcare.backend.repository.RoomRepository;
import com.roomcare.backend.repository.RoommateRepository;
import com.roomcare.backend.repository.TenantRepository;
import com.roomcare.backend.repository.UserRepository;
import com.roomcare.backend.repository.UtilityRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private static final DateTimeFormatter LOG_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final RoomRepository roomRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final UtilityRecordRepository utilityRecordRepository;
    private final NotificationService notificationService;

    @Autowired(required = false)
    private ContractRepository contractRepository;

    @Autowired(required = false)
    private RoommateRepository roommateRepository;

    @Transactional
    public List<InvoiceResponse> getInvoices(
            Long landlordId,
            String month,
            Long roomId,
            Long propertyId,
            InvoiceStatus status) {

        String normalizedMonth = (month != null && !month.isBlank()) ? month.trim() : null;
        if (normalizedMonth != null) {
            validateBillingMonth(normalizedMonth);
        }

        List<Invoice> invoices = invoiceRepository.findByLandlordWithFilters(
                landlordId, normalizedMonth, roomId, propertyId, null
        );

        boolean changed = false;
        LocalDate today = LocalDate.now();
        for (Invoice invoice : invoices) {
            if ((invoice.getStatus() == InvoiceStatus.UNPAID || invoice.getStatus() == InvoiceStatus.PARTIALLY_PAID)
                    && invoice.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0
                    && invoice.getDueDate().isBefore(today)) {
                invoice.setStatus(InvoiceStatus.OVERDUE);
                changed = true;
            }
        }
        if (changed) {
            invoiceRepository.saveAll(invoices);
        }

        return invoices.stream()
                .filter(inv -> status == null || inv.getStatus() == status)
                .map(inv -> mapToResponse(inv, false))
                .collect(Collectors.toList());
    }

    @Transactional
    public InvoiceResponse getInvoiceById(Long landlordId, Long invoiceId) {
        Invoice invoice = findOwnedInvoice(landlordId, invoiceId, "Bạn không có quyền truy cập hóa đơn này");

        if ((invoice.getStatus() == InvoiceStatus.UNPAID || invoice.getStatus() == InvoiceStatus.PARTIALLY_PAID)
                && invoice.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0
                && invoice.getDueDate().isBefore(LocalDate.now())) {
            invoice.setStatus(InvoiceStatus.OVERDUE);
            invoice = invoiceRepository.save(invoice);
        }

        return mapToResponse(invoice, true);
    }

    @Transactional(readOnly = true)
    public InvoiceResponse previewInvoice(Long landlordId, CreateInvoiceRequest request) {
        validateBillingMonth(request.getBillingMonth());
        Room room = findOwnedRoom(landlordId, request.getRoomId(), "Bạn không có quyền xem trước hóa đơn phòng này");
        User landlord = room.getProperty().getLandlord();
        Contract contract = resolveContractForInvoice(landlordId, room, request.getContractId(), request.getBillingMonth(), false);
        Invoice preview = assembleInvoiceEntity(null, landlord, room, contract, request);
        if (Boolean.TRUE.equals(request.getSaveAsDraft())) {
            preview.setStatus(InvoiceStatus.DRAFT);
            preview.setPublishedAt(null);
        }
        return mapToResponse(preview, false);
    }

    @Transactional
    public InvoiceResponse createInvoice(Long landlordId, CreateInvoiceRequest request) {
        validateBillingMonth(request.getBillingMonth());

        Room room = findOwnedRoom(landlordId, request.getRoomId(), "Bạn không có quyền lập hóa đơn cho phòng này");

        if (hasActiveInvoiceForMonth(room.getId(), request.getBillingMonth())) {
            throw new RuntimeException("Phòng " + room.getRoomCode() + " đã có hóa đơn cho kỳ " + request.getBillingMonth());
        }

        User landlord = userRepository.findById(landlordId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chủ nhà"));

        Contract contract = resolveContractForInvoice(landlordId, room, request.getContractId(), request.getBillingMonth(), true);
        Invoice invoice = assembleInvoiceEntity(null, landlord, room, contract, request);

        if (Boolean.TRUE.equals(request.getSaveAsDraft())) {
            invoice.setStatus(InvoiceStatus.DRAFT);
            invoice.setPublishedAt(null);
        } else {
            invoice.setStatus(resolveStatus(invoice.getTotalAmount(), BigDecimal.ZERO, invoice.getDueDate()));
            invoice.setPublishedAt(LocalDateTime.now());
        }

        Invoice saved = invoiceRepository.save(invoice);
        if (saved.getStatus() != InvoiceStatus.DRAFT) {
            notifyTenantInvoiceCreated(saved);
        }
        return mapToResponse(saved, false);
    }

    @Transactional
    public InvoiceResponse updateDraftInvoice(Long landlordId, Long invoiceId, CreateInvoiceRequest request) {
        validateBillingMonth(request.getBillingMonth());
        Invoice existing = findOwnedInvoice(landlordId, invoiceId, "Bạn không có quyền sửa hóa đơn này");
        if (existing.getStatus() != InvoiceStatus.DRAFT) {
            throw new RuntimeException("Chỉ hóa đơn ở trạng thái Nháp (DRAFT) mới được chỉnh sửa trực tiếp");
        }

        Room room = findOwnedRoom(landlordId, request.getRoomId(), "Bạn không có quyền lập hóa đơn cho phòng này");
        if ((!Objects.equals(existing.getRoom().getId(), room.getId())
                || !Objects.equals(existing.getBillingMonth(), request.getBillingMonth()))
                && hasActiveInvoiceForMonth(room.getId(), request.getBillingMonth())) {
            throw new RuntimeException("Phòng " + room.getRoomCode() + " đã có hóa đơn cho kỳ " + request.getBillingMonth());
        }

        Contract contract = resolveContractForInvoice(landlordId, room, request.getContractId(), request.getBillingMonth(), true);
        Invoice updated = assembleInvoiceEntity(existing, existing.getLandlord(), room, contract, request);
        updated.setStatus(InvoiceStatus.DRAFT);
        updated.setPublishedAt(null);
        Invoice saved = invoiceRepository.save(updated);
        return mapToResponse(saved, true);
    }

    @Transactional
    public InvoiceResponse publishInvoice(Long landlordId, Long invoiceId) {
        Invoice invoice = findOwnedInvoice(landlordId, invoiceId, "Bạn không có quyền phát hành hóa đơn này");
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            throw new RuntimeException("Hóa đơn đã được phát hành hoặc đã hủy");
        }

        if (invoice.getContract() != null) {
            validateContractEligibleForBilling(invoice.getContract(), invoice.getBillingMonth());
        } else {
            Contract activeContract = resolveContractForInvoice(
                    landlordId, invoice.getRoom(), null, invoice.getBillingMonth(), true
            );
            if (activeContract != null) {
                invoice.setContract(activeContract);
            }
        }

        invoice.setPublishedAt(LocalDateTime.now());
        invoice.setStatus(resolveStatus(invoice.getTotalAmount(), invoice.getPaidAmount(), invoice.getDueDate()));
        Invoice saved = invoiceRepository.save(invoice);
        notifyTenantInvoiceCreated(saved);
        return mapToResponse(saved, true);
    }

    @Transactional
    public InvoiceResponse adjustInvoice(Long landlordId, Long invoiceId, AdjustInvoiceRequest request) {
        Invoice invoice = findOwnedInvoice(landlordId, invoiceId, "Bạn không có quyền điều chỉnh hóa đơn này");
        if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new RuntimeException("Không thể điều chỉnh hóa đơn đã thanh toán đủ hoặc đã hủy");
        }
        String reason = request.getAdjustmentReason() != null ? request.getAdjustmentReason().trim() : "";
        if (reason.isEmpty()) {
            throw new RuntimeException("Vui lòng nhập lý do điều chỉnh hóa đơn");
        }

        BigDecimal oldTotal = safeMoney(invoice.getTotalAmount());
        BigDecimal delta = request.getAdjustmentAmount() != null ? request.getAdjustmentAmount() : BigDecimal.ZERO;
        BigDecimal currentAdj = invoice.getAdjustmentAmount() != null ? invoice.getAdjustmentAmount() : BigDecimal.ZERO;
        BigDecimal newAdj = currentAdj.add(delta);

        if (request.getGarbageFee() != null) {
            invoice.setGarbageFee(safeMoney(request.getGarbageFee()));
        }
        if (request.getOtherFees() != null) {
            invoice.setOtherFees(safeMoney(request.getOtherFees()));
        }
        if (request.getDiscountAmount() != null) {
            invoice.setDiscountAmount(safeMoney(request.getDiscountAmount()));
        }
        if (request.getDueDate() != null) {
            invoice.setDueDate(request.getDueDate());
        }
        if (request.getNotes() != null) {
            invoice.setNotes(request.getNotes().trim().isEmpty() ? null : request.getNotes().trim());
        }

        invoice.setAdjustmentAmount(newAdj);
        invoice.setAdjustmentReason(reason);

        BigDecimal subtotal = safeMoney(invoice.getRoomRent())
                .add(safeMoney(invoice.getElectricityAmount()))
                .add(safeMoney(invoice.getWaterAmount()))
                .add(safeMoney(invoice.getInternetAmount()))
                .add(safeMoney(invoice.getGarbageFee()))
                .add(safeMoney(invoice.getServiceAmount()))
                .add(safeMoney(invoice.getOtherFees()))
                .add(safeMoney(invoice.getPreviousDebt()))
                .add(newAdj);

        BigDecimal discount = safeMoney(invoice.getDiscountAmount());
        if (subtotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Tổng khoản phí sau điều chỉnh không được âm");
        }
        if (discount.compareTo(subtotal) > 0) {
            throw new RuntimeException("Số tiền giảm trừ không được vượt quá tổng khoản phí");
        }

        BigDecimal newTotal = subtotal.subtract(discount);
        BigDecimal paidAmount = safeMoney(invoice.getPaidAmount());
        if (newTotal.compareTo(paidAmount) < 0) {
            throw new RuntimeException("Tổng tiền sau điều chỉnh không được nhỏ hơn số tiền đã thu (" + paidAmount.toPlainString() + " đ)");
        }

        invoice.setTotalAmount(newTotal);
        invoice.setRemainingAmount(newTotal.subtract(paidAmount));
        if (invoice.getStatus() != InvoiceStatus.DRAFT) {
            invoice.setStatus(resolveStatus(newTotal, paidAmount, invoice.getDueDate()));
        }

        String logEntry = "[" + LocalDateTime.now().format(LOG_FMT) + "] Điều chỉnh "
                + (delta.compareTo(BigDecimal.ZERO) >= 0 ? "+" : "") + delta.stripTrailingZeros().toPlainString()
                + "đ (" + oldTotal.stripTrailingZeros().toPlainString() + "đ -> "
                + newTotal.stripTrailingZeros().toPlainString() + "đ) | Lý do: " + reason;
        String history = invoice.getAdjustmentHistory();
        invoice.setAdjustmentHistory(history == null || history.isBlank() ? logEntry : logEntry + "\n" + history);

        Invoice saved = invoiceRepository.save(invoice);
        if (saved.getStatus() != InvoiceStatus.DRAFT
                && notificationService != null
                && saved.getTenant() != null
                && saved.getTenant().getUser() != null) {
            notificationService.createNotification(
                    saved.getTenant().getUser(),
                    NotificationType.INVOICE,
                    "Điều chỉnh hóa đơn " + saved.getInvoiceCode(),
                    "Hóa đơn kỳ " + saved.getBillingMonth() + " đã được cập nhật tổng tiền thành "
                            + saved.getTotalAmount().toPlainString() + " đ. Lý do: " + reason,
                    saved.getId()
            );
        }
        return mapToResponse(saved, true);
    }

    @Transactional
    public InvoiceResponse cancelInvoice(Long landlordId, Long invoiceId, CancelInvoiceRequest request) {
        Invoice invoice = findOwnedInvoice(landlordId, invoiceId, "Bạn không có quyền hủy hóa đơn này");
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new RuntimeException("Hóa đơn này đã bị hủy trước đó");
        }
        if (invoice.getStatus() == InvoiceStatus.PAID
                || invoice.getStatus() == InvoiceStatus.PARTIALLY_PAID
                || (invoice.getPaidAmount() != null && invoice.getPaidAmount().compareTo(BigDecimal.ZERO) > 0)) {
            throw new RuntimeException("Không thể hủy hóa đơn đã phát sinh thanh toán");
        }

        String reason = request != null && request.getReason() != null && !request.getReason().isBlank()
                ? request.getReason().trim()
                : "Chủ nhà hủy hóa đơn";
        boolean wasPublished = invoice.getStatus() != InvoiceStatus.DRAFT || invoice.getPublishedAt() != null;

        invoice.setStatus(InvoiceStatus.CANCELLED);
        invoice.setCancelledAt(LocalDateTime.now());
        invoice.setCancellationReason(reason);
        invoice.setRemainingAmount(BigDecimal.ZERO);

        Invoice saved = invoiceRepository.save(invoice);
        if (wasPublished
                && notificationService != null
                && saved.getTenant() != null
                && saved.getTenant().getUser() != null) {
            notificationService.createNotification(
                    saved.getTenant().getUser(),
                    NotificationType.INVOICE,
                    "Hủy hóa đơn " + saved.getInvoiceCode(),
                    "Hóa đơn kỳ " + saved.getBillingMonth() + " của phòng " + saved.getRoom().getRoomCode()
                            + " đã được hủy. Lý do: " + reason,
                    saved.getId()
            );
        }
        return mapToResponse(saved, true);
    }

    @Transactional
    public InvoiceResponse recordPayment(Long landlordId, Long invoiceId, CreatePaymentRequest request) {
        if (!Boolean.TRUE.equals(request.getConfirmedByLandlord())) {
            throw new RuntimeException("Chỉ chủ nhà xác nhận thực thu mới được ghi nhận thanh toán hóa đơn");
        }

        Invoice invoice = findOwnedInvoice(landlordId, invoiceId, "Bạn không có quyền ghi nhận thanh toán cho hóa đơn này");

        if (invoice.getStatus() == InvoiceStatus.DRAFT) {
            throw new RuntimeException("Hóa đơn ở trạng thái Nháp chưa thể thu tiền, vui lòng phát hành hóa đơn trước");
        }
        if (invoice.getStatus() == InvoiceStatus.CANCELLED) {
            throw new RuntimeException("Hóa đơn đã hủy, không thể ghi nhận thanh toán");
        }
        if (invoice.getStatus() == InvoiceStatus.PAID || invoice.getRemainingAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Hóa đơn này đã được thanh toán đủ");
        }

        BigDecimal amount = safeMoney(request.getAmount());
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Số tiền thanh toán phải lớn hơn 0");
        }

        if (amount.compareTo(invoice.getRemainingAmount()) > 0) {
            throw new RuntimeException("Số tiền thanh toán không được vượt quá số tiền còn thiếu ("
                    + invoice.getRemainingAmount().toPlainString() + " đ)");
        }

        String trimmedCode = request.getTransactionCode() != null && !request.getTransactionCode().isBlank()
                ? request.getTransactionCode().trim()
                : null;

        if (trimmedCode != null && paymentRepository.existsByInvoiceIdAndTransactionCode(invoiceId, trimmedCode)) {
            throw new RuntimeException("Mã giao dịch " + trimmedCode + " đã tồn tại cho hóa đơn này");
        }

        LocalDateTime now = LocalDateTime.now();
        if (paymentRepository.existsByInvoiceIdAndAmountAndPaymentMethodAndPaidAtAfter(
                invoiceId, amount, request.getPaymentMethod(), now.minusMinutes(2))) {
            throw new RuntimeException("Giao dịch tương tự vừa được ghi nhận, vui lòng kiểm tra lại để tránh trùng");
        }

        User landlord = userRepository.findById(landlordId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chủ nhà"));

        Payment payment = new Payment();
        payment.setInvoice(invoice);
        payment.setLandlord(landlord);
        payment.setAmount(amount);
        payment.setPaymentMethod(request.getPaymentMethod());
        payment.setTransactionCode(trimmedCode != null ? trimmedCode : generateReceiptCode(invoice));
        payment.setReceiptNote(request.getReceiptNote());
        payment.setPaidAt(now);
        paymentRepository.save(payment);

        BigDecimal newPaidAmount = invoice.getPaidAmount().add(amount);
        BigDecimal newRemainingAmount = invoice.getTotalAmount().subtract(newPaidAmount);
        if (newRemainingAmount.compareTo(BigDecimal.ZERO) < 0) {
            newRemainingAmount = BigDecimal.ZERO;
        }

        invoice.setPaidAmount(newPaidAmount);
        invoice.setRemainingAmount(newRemainingAmount);
        invoice.setStatus(resolveStatus(invoice.getTotalAmount(), newPaidAmount, invoice.getDueDate()));
        Invoice savedInvoice = invoiceRepository.save(invoice);

        if (notificationService != null
                && savedInvoice.getTenant() != null
                && savedInvoice.getTenant().getUser() != null) {
            notificationService.createNotification(
                    savedInvoice.getTenant().getUser(),
                    NotificationType.PAYMENT,
                    "Xác nhận thanh toán hóa đơn",
                    "Chủ nhà đã xác nhận thanh toán " + amount.toPlainString() + " đ cho hóa đơn "
                            + savedInvoice.getInvoiceCode() + ".",
                    savedInvoice.getId()
            );
        }

        return mapToResponse(savedInvoice, true);
    }

    @Transactional
    public BatchCreateInvoiceResult batchCreateInvoices(Long landlordId, BatchCreateInvoiceRequest request) {
        validateBillingMonth(request.getBillingMonth());

        User landlord = userRepository.findById(landlordId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chủ nhà"));

        List<Room> targetRooms = resolveTargetRooms(landlordId, request);
        if (targetRooms.isEmpty()) {
            throw new RuntimeException("Không có phòng phù hợp để tạo hóa đơn hàng loạt");
        }

        List<InvoiceResponse> created = new ArrayList<>();
        List<String> skippedReasons = new ArrayList<>();

        for (Room room : targetRooms) {
            if (hasActiveInvoiceForMonth(room.getId(), request.getBillingMonth())) {
                skippedReasons.add("Phòng " + room.getRoomCode() + ": đã tồn tại hóa đơn kỳ " + request.getBillingMonth());
                continue;
            }

            Contract contract;
            try {
                contract = resolveContractForInvoice(landlordId, room, null, request.getBillingMonth(), true);
            } catch (RuntimeException ex) {
                skippedReasons.add("Phòng " + room.getRoomCode() + ": " + ex.getMessage());
                continue;
            }

            CreateInvoiceRequest itemReq = new CreateInvoiceRequest();
            itemReq.setRoomId(room.getId());
            itemReq.setContractId(contract != null ? contract.getId() : null);
            itemReq.setBillingMonth(request.getBillingMonth());
            itemReq.setDueDate(request.getDueDate());
            if (request.getDefaultInternetFee() != null && request.getDefaultInternetFee().compareTo(BigDecimal.ZERO) > 0) {
                itemReq.setInternetAmount(request.getDefaultInternetFee());
            }
            if (request.getDefaultServiceFee() != null && request.getDefaultServiceFee().compareTo(BigDecimal.ZERO) > 0) {
                itemReq.setServiceAmount(request.getDefaultServiceFee());
            }
            itemReq.setOtherFees(request.getOtherFees());
            itemReq.setNotes(request.getNotes());
            itemReq.setUseUtilityRecord(Boolean.TRUE);
            itemReq.setAutoCarryPreviousDebt(Boolean.FALSE);

            Invoice invoice = assembleInvoiceEntity(null, landlord, room, contract, itemReq);
            invoice.setStatus(resolveStatus(invoice.getTotalAmount(), BigDecimal.ZERO, invoice.getDueDate()));
            invoice.setPublishedAt(LocalDateTime.now());

            Invoice saved = invoiceRepository.save(invoice);
            notifyTenantInvoiceCreated(saved);
            created.add(mapToResponse(saved, false));
        }

        return new BatchCreateInvoiceResult(
                created.size(),
                skippedReasons.size(),
                created,
                skippedReasons
        );
    }

    private Invoice assembleInvoiceEntity(
            Invoice target,
            User landlord,
            Room room,
            Contract contract,
            CreateInvoiceRequest request) {
        Tenant tenant = resolveTenantForRoom(landlord.getId(), room, request.getTenantId(), contract);
        String billingMonth = request.getBillingMonth();

        Optional<UtilityRecord> utilityOpt = utilityRecordRepository != null
                ? utilityRecordRepository.findByRoomIdAndBillingMonth(room.getId(), billingMonth)
                : Optional.empty();
        boolean useUtility = !Boolean.FALSE.equals(request.getUseUtilityRecord()) && utilityOpt.isPresent();
        Long utilityRecordId = utilityOpt.map(UtilityRecord::getId).orElse(null);

        int occupantCount = resolveOccupantCount(request, room, contract, tenant);

        BigDecimal roomRent = (request.getRoomRent() != null && request.getRoomRent().compareTo(BigDecimal.ZERO) > 0)
                ? request.getRoomRent()
                : (contract != null && contract.getMonthlyRent() != null
                        ? safeMoney(contract.getMonthlyRent())
                        : safeMoney(room.getRentPrice()));

        // Điện: ưu tiên chỉ số nhập trực tiếp -> UtilityRecord -> request.electricityAmount
        BigDecimal elecUnitPrice = request.getElectricUnitPrice() != null
                ? safeMoney(request.getElectricUnitPrice())
                : (useUtility && utilityOpt.get().getElectricUnitPrice() != null
                        && utilityOpt.get().getElectricUnitPrice().compareTo(BigDecimal.ZERO) > 0
                        ? utilityOpt.get().getElectricUnitPrice()
                        : (contract != null && contract.getElectricityUnitPrice() != null
                                ? safeMoney(contract.getElectricityUnitPrice())
                                : safeMoney(room.getElectricityPrice())));
        BigDecimal prevElec = useUtility ? utilityOpt.get().getPrevElectricIndex() : null;
        BigDecimal currElec = useUtility ? utilityOpt.get().getCurrElectricIndex() : null;
        BigDecimal elecUsage = useUtility ? utilityOpt.get().getElectricUsage() : null;
        BigDecimal electricity;

        if (request.getCurrElectricIndex() != null || request.getPrevElectricIndex() != null) {
            prevElec = safeMoney(request.getPrevElectricIndex() != null
                    ? request.getPrevElectricIndex()
                    : (prevElec != null ? prevElec : BigDecimal.ZERO));
            currElec = safeMoney(request.getCurrElectricIndex() != null
                    ? request.getCurrElectricIndex()
                    : (currElec != null ? currElec : prevElec));
            if (currElec.compareTo(prevElec) < 0) {
                throw new RuntimeException("Chỉ số điện mới phải lớn hơn hoặc bằng chỉ số điện cũ");
            }
            elecUsage = currElec.subtract(prevElec);
            electricity = request.getElectricityAmount() != null
                    ? safeMoney(request.getElectricityAmount())
                    : elecUsage.multiply(elecUnitPrice);
        } else if (request.getElectricityAmount() != null
                && (!useUtility || request.getElectricityAmount().compareTo(BigDecimal.ZERO) > 0)) {
            electricity = safeMoney(request.getElectricityAmount());
        } else if (useUtility) {
            electricity = utilityOpt.get().getElectricityAmount();
        } else {
            electricity = BigDecimal.ZERO;
        }

        // Nước: hỗ trợ METERED / FIXED / PER_PERSON
        WaterBillingMode waterMode = request.getWaterBillingMode() != null
                ? request.getWaterBillingMode()
                : (contract != null && contract.getWaterBillingMode() != null
                        ? contract.getWaterBillingMode()
                        : WaterBillingMode.METERED);
        BigDecimal waterUnitPrice = request.getWaterUnitPrice() != null
                ? safeMoney(request.getWaterUnitPrice())
                : (useUtility && utilityOpt.get().getWaterUnitPrice() != null
                        && utilityOpt.get().getWaterUnitPrice().compareTo(BigDecimal.ZERO) > 0
                        ? utilityOpt.get().getWaterUnitPrice()
                        : (contract != null && contract.getWaterUnitPrice() != null
                                ? safeMoney(contract.getWaterUnitPrice())
                                : safeMoney(room.getWaterPrice())));
        BigDecimal prevWater = useUtility ? utilityOpt.get().getPrevWaterIndex() : null;
        BigDecimal currWater = useUtility ? utilityOpt.get().getCurrWaterIndex() : null;
        BigDecimal waterUsage = useUtility ? utilityOpt.get().getWaterUsage() : null;
        BigDecimal water;

        boolean explicitWaterIndices = request.getCurrWaterIndex() != null || request.getPrevWaterIndex() != null;
        if (explicitWaterIndices) {
            prevWater = safeMoney(request.getPrevWaterIndex() != null
                    ? request.getPrevWaterIndex()
                    : (prevWater != null ? prevWater : BigDecimal.ZERO));
            currWater = safeMoney(request.getCurrWaterIndex() != null
                    ? request.getCurrWaterIndex()
                    : (currWater != null ? currWater : prevWater));
            if (currWater.compareTo(prevWater) < 0) {
                throw new RuntimeException("Chỉ số nước mới phải lớn hơn hoặc bằng chỉ số nước cũ");
            }
            waterUsage = currWater.subtract(prevWater);
            water = request.getWaterAmount() != null
                    ? safeMoney(request.getWaterAmount())
                    : waterUsage.multiply(waterUnitPrice);
        } else if (request.getWaterAmount() != null
                && (!useUtility || request.getWaterAmount().compareTo(BigDecimal.ZERO) > 0)) {
            water = safeMoney(request.getWaterAmount());
        } else if (useUtility && request.getWaterBillingMode() == null
                && utilityOpt.get().getWaterAmount() != null
                && utilityOpt.get().getWaterAmount().compareTo(BigDecimal.ZERO) > 0) {
            water = utilityOpt.get().getWaterAmount();
        } else if (waterMode == WaterBillingMode.FIXED) {
            waterUsage = BigDecimal.ONE;
            water = waterUnitPrice;
        } else if (waterMode == WaterBillingMode.PER_PERSON) {
            waterUsage = BigDecimal.valueOf(occupantCount);
            water = waterUnitPrice.multiply(waterUsage);
        } else if (useUtility) {
            water = utilityOpt.get().getWaterAmount();
        } else {
            water = BigDecimal.ZERO;
        }

        BigDecimal internet = (request.getInternetAmount() != null
                && request.getInternetAmount().compareTo(BigDecimal.ZERO) > 0)
                ? safeMoney(request.getInternetAmount())
                : (useUtility
                        ? utilityOpt.get().getInternetFee()
                        : (contract != null && contract.getInternetFee() != null
                                ? safeMoney(contract.getInternetFee())
                                : (request.getInternetAmount() != null
                                        ? safeMoney(request.getInternetAmount())
                                        : safeMoney(room.getInternetFee()))));

        BigDecimal service = (request.getServiceAmount() != null
                && request.getServiceAmount().compareTo(BigDecimal.ZERO) > 0)
                ? safeMoney(request.getServiceAmount())
                : (useUtility
                        ? utilityOpt.get().getServiceFee()
                        : (contract != null && contract.getServiceFee() != null
                                ? safeMoney(contract.getServiceFee())
                                : (request.getServiceAmount() != null
                                        ? safeMoney(request.getServiceAmount())
                                        : safeMoney(room.getServiceFee()))));

        BigDecimal garbageFee = (request.getGarbageFee() != null
                && request.getGarbageFee().compareTo(BigDecimal.ZERO) > 0)
                ? safeMoney(request.getGarbageFee())
                : (contract != null && contract.getGarbageFee() != null
                        ? safeMoney(contract.getGarbageFee())
                        : safeMoney(request.getGarbageFee()));

        BigDecimal otherFees = safeMoney(request.getOtherFees());

        BigDecimal previousDebt = request.getPreviousDebt() != null
                ? safeMoney(request.getPreviousDebt())
                : (Boolean.TRUE.equals(request.getAutoCarryPreviousDebt())
                        ? calculatePreviousDebt(room.getId(), billingMonth)
                        : BigDecimal.ZERO);

        BigDecimal adjustmentAmount = request.getAdjustmentAmount() != null
                ? request.getAdjustmentAmount()
                : BigDecimal.ZERO;

        BigDecimal discount = safeMoney(request.getDiscountAmount());

        Invoice invoice = target != null ? target : new Invoice();
        if (target == null) {
            invoice.setInvoiceCode(generateInvoiceCode(room, billingMonth));
            invoice.setPaidAmount(BigDecimal.ZERO);
        }

        BigDecimal subtotal = roomRent
                .add(electricity)
                .add(water)
                .add(internet)
                .add(garbageFee)
                .add(service)
                .add(otherFees)
                .add(previousDebt)
                .add(adjustmentAmount);

        if (subtotal.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Tổng khoản phí sau điều chỉnh không được âm");
        }
        if (discount.compareTo(subtotal) > 0) {
            throw new RuntimeException("Số tiền giảm trừ không được lớn hơn tổng khoản phí");
        }

        BigDecimal totalAmount = subtotal.subtract(discount);
        BigDecimal paid = safeMoney(invoice.getPaidAmount());

        invoice.setLandlord(landlord);
        invoice.setProperty(room.getProperty());
        invoice.setRoom(room);
        invoice.setTenant(tenant);
        invoice.setContract(contract);
        invoice.setUtilityRecordId(utilityRecordId);
        invoice.setPrevElectricIndex(prevElec);
        invoice.setCurrElectricIndex(currElec);
        invoice.setElectricUsage(elecUsage);
        invoice.setElectricUnitPrice(elecUnitPrice);
        invoice.setPrevWaterIndex(prevWater);
        invoice.setCurrWaterIndex(currWater);
        invoice.setWaterUsage(waterUsage);
        invoice.setWaterUnitPrice(waterUnitPrice);
        invoice.setWaterBillingMode(waterMode);
        invoice.setOccupantCount(occupantCount);
        invoice.setBillingMonth(billingMonth);
        invoice.setRoomRent(roomRent);
        invoice.setElectricityAmount(electricity);
        invoice.setWaterAmount(water);
        invoice.setInternetAmount(internet);
        invoice.setGarbageFee(garbageFee);
        invoice.setServiceAmount(service);
        invoice.setOtherFees(otherFees);
        invoice.setPreviousDebt(previousDebt);
        invoice.setAdjustmentAmount(adjustmentAmount);
        invoice.setAdjustmentReason(request.getAdjustmentReason());
        invoice.setDiscountAmount(discount);
        invoice.setTotalAmount(totalAmount);
        invoice.setRemainingAmount(totalAmount.subtract(paid).max(BigDecimal.ZERO));
        invoice.setDueDate(request.getDueDate());
        invoice.setNotes(request.getNotes());
        invoice.setStatus(resolveStatus(totalAmount, paid, request.getDueDate()));
        return invoice;
    }

    private int resolveOccupantCount(CreateInvoiceRequest request, Room room, Contract contract, Tenant tenant) {
        if (request.getOccupantCount() != null && request.getOccupantCount() > 0) {
            return request.getOccupantCount();
        }
        if (roommateRepository == null) {
            return 1;
        }
        List<Roommate> roommates = contract != null
                ? roommateRepository.findByContractIdOrderByCreatedAtAsc(contract.getId())
                : (tenant != null
                        ? roommateRepository.findByTenantIdOrderByCreatedAtAsc(tenant.getId())
                        : roommateRepository.findByRoomIdOrderByCreatedAtAsc(room.getId()));
        if (roommates == null || roommates.isEmpty()) {
            return 1;
        }
        long nonRepresentativeCount = roommates.stream()
                .filter(r -> !Boolean.TRUE.equals(r.getIsRepresentative()))
                .count();
        long baseTenant = tenant != null ? 1 : 0;
        return (int) Math.max(1L, baseTenant + nonRepresentativeCount);
    }

    private Contract resolveContractForInvoice(
            Long landlordId,
            Room room,
            Long requestedContractId,
            String billingMonth,
            boolean enforceActive) {
        if (contractRepository == null) {
            return null;
        }
        if (requestedContractId != null) {
            Contract contract = contractRepository.findById(requestedContractId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng"));
            if (!contract.getLandlord().getId().equals(landlordId)) {
                throw new RuntimeException("Hợp đồng không thuộc quyền quản lý của bạn");
            }
            if (!contract.getRoom().getId().equals(room.getId())) {
                throw new RuntimeException("Hợp đồng không gắn với phòng " + room.getRoomCode());
            }
            if (enforceActive) {
                validateContractEligibleForBilling(contract, billingMonth);
            }
            return contract;
        }

        Optional<Contract> activeOpt = contractRepository.findFirstByRoomIdAndStatusInOrderByActivatedAtDescCreatedAtDesc(
                room.getId(), List.of(ContractStatus.ACTIVE)
        );
        if (activeOpt.isPresent()) {
            Contract active = activeOpt.get();
            if (enforceActive) {
                validateContractEligibleForBilling(active, billingMonth);
            }
            return active;
        }

        if (!enforceActive) {
            return contractRepository.findByRoomIdOrderByStartDateDescCreatedAtDesc(room.getId())
                    .stream().findFirst().orElse(null);
        }

        List<Contract> allRoomContracts = contractRepository.findByRoomIdOrderByStartDateDescCreatedAtDesc(room.getId());
        if (allRoomContracts != null && !allRoomContracts.isEmpty()) {
            throw new RuntimeException("Phòng " + room.getRoomCode() + " chưa có hợp đồng ở trạng thái ACTIVE để lập hóa đơn");
        }

        // Cho phép tiếp nối dữ liệu cũ nếu phòng chưa dùng bảng contracts nhưng đã có hóa đơn kế thừa trước đó
        List<Invoice> priorInvoices = invoiceRepository.findByRoomIdOrderByBillingMonthDesc(room.getId());
        if (priorInvoices == null || priorInvoices.isEmpty()) {
            throw new RuntimeException("Phòng " + room.getRoomCode() + " chưa có hợp đồng thuê hiệu lực để lập hóa đơn");
        }
        return null;
    }

    private void validateContractEligibleForBilling(Contract contract, String billingMonth) {
        if (contract.getStatus() != ContractStatus.ACTIVE) {
            throw new RuntimeException("Hợp đồng " + contract.getContractCode() + " chưa ở trạng thái hiệu lực (ACTIVE)");
        }
        YearMonth ym = YearMonth.parse(billingMonth);
        LocalDate monthStart = ym.atDay(1);
        LocalDate monthEnd = ym.atEndOfMonth();
        if (contract.getStartDate().isAfter(monthEnd) || contract.getEndDate().isBefore(monthStart)) {
            throw new RuntimeException("Kỳ hóa đơn " + billingMonth + " nằm ngoài thời hạn hợp đồng ("
                    + contract.getStartDate() + " đến " + contract.getEndDate() + ")");
        }
    }

    private boolean hasActiveInvoiceForMonth(Long roomId, String billingMonth) {
        List<Invoice> existing = invoiceRepository.findAllByRoomIdAndBillingMonthOrderByCreatedAtDesc(roomId, billingMonth);
        if (existing != null && !existing.isEmpty()) {
            return existing.stream().anyMatch(i -> i.getStatus() != InvoiceStatus.CANCELLED);
        }
        return invoiceRepository.existsByRoomIdAndBillingMonth(roomId, billingMonth);
    }

    private Room findOwnedRoom(Long landlordId, Long roomId, String unauthorizedMessage) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));
        if (!room.getProperty().getLandlord().getId().equals(landlordId)) {
            throw new RuntimeException(unauthorizedMessage);
        }
        return room;
    }

    private Invoice findOwnedInvoice(Long landlordId, Long invoiceId, String unauthorizedMessage) {
        Invoice invoice = invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hóa đơn"));
        if (!invoice.getLandlord().getId().equals(landlordId)) {
            throw new RuntimeException(unauthorizedMessage);
        }
        return invoice;
    }

    private void notifyTenantInvoiceCreated(Invoice saved) {
        if (notificationService != null
                && saved.getTenant() != null
                && saved.getTenant().getUser() != null) {
            notificationService.createNotification(
                    saved.getTenant().getUser(),
                    NotificationType.INVOICE,
                    "Hóa đơn mới kỳ " + saved.getBillingMonth(),
                    "Phòng " + saved.getRoom().getRoomCode() + " có hóa đơn " + saved.getInvoiceCode()
                            + " tổng tiền " + saved.getTotalAmount().toPlainString() + " đ.",
                    saved.getId()
            );
        }
    }

    protected List<Room> resolveTargetRooms(Long landlordId, BatchCreateInvoiceRequest request) {
        List<Room> candidateRooms;
        if (request.getRoomIds() != null && !request.getRoomIds().isEmpty()) {
            candidateRooms = roomRepository.findAllById(request.getRoomIds());
            for (Room room : candidateRooms) {
                if (!room.getProperty().getLandlord().getId().equals(landlordId)) {
                    throw new RuntimeException("Có phòng không thuộc quyền quản lý của bạn");
                }
            }
        } else if (request.getPropertyId() != null) {
            candidateRooms = roomRepository.findByPropertyId(
                    request.getPropertyId(), PageRequest.of(0, 500)
            ).getContent();
            for (Room room : candidateRooms) {
                if (!room.getProperty().getLandlord().getId().equals(landlordId)) {
                    throw new RuntimeException("Bạn không có quyền truy cập nhà trọ này");
                }
            }
        } else {
            candidateRooms = roomRepository.findByLandlordId(
                    landlordId, PageRequest.of(0, 500)
            ).getContent();
        }

        boolean onlyOccupied = request.getOnlyOccupiedRooms() == null || request.getOnlyOccupiedRooms();
        if (!onlyOccupied || (request.getRoomIds() != null && !request.getRoomIds().isEmpty())) {
            return candidateRooms;
        }

        return candidateRooms.stream()
                .filter(room -> room.getStatus() == RoomStatus.OCCUPIED
                        || room.getStatus() == RoomStatus.EXPIRING_SOON)
                .collect(Collectors.toList());
    }

    protected BigDecimal calculatePreviousDebt(Long roomId, String billingMonth) {
        List<Invoice> unpaidPrevious = invoiceRepository
                .findByRoomIdAndBillingMonthLessThanAndRemainingAmountGreaterThan(
                        roomId, billingMonth, BigDecimal.ZERO
                );
        return unpaidPrevious.stream()
                .filter(inv -> inv.getStatus() != InvoiceStatus.DRAFT && inv.getStatus() != InvoiceStatus.CANCELLED)
                .map(Invoice::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    protected Tenant resolveTenantForRoom(Long landlordId, Room room, Long requestedTenantId) {
        return resolveTenantForRoom(landlordId, room, requestedTenantId, null);
    }

    protected Tenant resolveTenantForRoom(Long landlordId, Room room, Long requestedTenantId, Contract contract) {
        if (requestedTenantId != null) {
            Tenant tenant = tenantRepository.findById(requestedTenantId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người thuê"));
            if (!tenant.getLandlord().getId().equals(landlordId)) {
                throw new RuntimeException("Người thuê không thuộc quyền quản lý của bạn");
            }
            return tenant;
        }

        if (contract != null && contract.getTenant() != null) {
            return contract.getTenant();
        }

        List<Invoice> priorInvoices = invoiceRepository.findByRoomIdOrderByBillingMonthDesc(room.getId());
        for (Invoice prior : priorInvoices) {
            if (prior.getTenant() != null && prior.getStatus() != InvoiceStatus.CANCELLED) {
                return prior.getTenant();
            }
        }
        return null;
    }

    protected InvoiceStatus resolveStatus(BigDecimal totalAmount, BigDecimal paidAmount, LocalDate dueDate) {
        BigDecimal remaining = totalAmount.subtract(paidAmount);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            return InvoiceStatus.PAID;
        }
        if (dueDate.isBefore(LocalDate.now())) {
            return InvoiceStatus.OVERDUE;
        }
        if (paidAmount.compareTo(BigDecimal.ZERO) > 0) {
            return InvoiceStatus.PARTIALLY_PAID;
        }
        return InvoiceStatus.UNPAID;
    }

    protected void validateBillingMonth(String billingMonth) {
        if (billingMonth == null || !billingMonth.matches("^\\d{4}-(0[1-9]|1[0-2])$")) {
            throw new RuntimeException("Kỳ hóa đơn không hợp lệ (định dạng chuẩn YYYY-MM)");
        }
        YearMonth.parse(billingMonth);
    }

    private String generateInvoiceCode(Room room, String billingMonth) {
        String compactMonth = billingMonth.replace("-", "");
        String sanitizedRoom = room.getRoomCode().replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        List<Invoice> existing = invoiceRepository.findAllByRoomIdAndBillingMonthOrderByCreatedAtDesc(room.getId(), billingMonth);
        String suffix = (existing != null && !existing.isEmpty()) ? "-V" + (existing.size() + 1) : "";
        String baseCode = "INV-" + compactMonth + "-" + sanitizedRoom + "-R" + room.getId() + suffix;
        if (baseCode.length() > 40) {
            return baseCode.substring(0, 40);
        }
        return baseCode;
    }

    protected BigDecimal safeMoney(BigDecimal value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException("Giá trị tiền không được âm");
        }
        return value;
    }

    private String generateReceiptCode(Invoice invoice) {
        return "PT-" + invoice.getId() + "-" + System.currentTimeMillis() % 100000;
    }

    protected InvoiceResponse mapToResponse(Invoice invoice) {
        return mapToResponse(invoice, false);
    }

    protected InvoiceResponse mapToResponse(Invoice invoice, boolean includePayments) {
        InvoiceResponse response = new InvoiceResponse();
        response.setId(invoice.getId());
        response.setInvoiceCode(invoice.getInvoiceCode());
        response.setPropertyId(invoice.getProperty().getId());
        response.setPropertyName(invoice.getProperty().getName());
        response.setRoomId(invoice.getRoom().getId());
        response.setRoomCode(invoice.getRoom().getRoomCode());
        if (invoice.getTenant() != null) {
            response.setTenantId(invoice.getTenant().getId());
            response.setTenantName(invoice.getTenant().getFullName());
            response.setTenantPhone(invoice.getTenant().getPhone());
        }
        if (invoice.getContract() != null) {
            response.setContractId(invoice.getContract().getId());
            response.setContractCode(invoice.getContract().getContractCode());
        }
        response.setUtilityRecordId(invoice.getUtilityRecordId());
        response.setBillingMonth(invoice.getBillingMonth());
        response.setRoomRent(invoice.getRoomRent());
        response.setElectricityAmount(invoice.getElectricityAmount());
        response.setWaterAmount(invoice.getWaterAmount());
        response.setInternetAmount(invoice.getInternetAmount());
        response.setGarbageFee(invoice.getGarbageFee() != null ? invoice.getGarbageFee() : BigDecimal.ZERO);
        response.setServiceAmount(invoice.getServiceAmount());
        response.setOtherFees(invoice.getOtherFees());
        response.setPreviousDebt(invoice.getPreviousDebt());
        response.setAdjustmentAmount(invoice.getAdjustmentAmount() != null ? invoice.getAdjustmentAmount() : BigDecimal.ZERO);
        response.setAdjustmentReason(invoice.getAdjustmentReason());
        response.setAdjustmentHistory(invoice.getAdjustmentHistory());
        response.setDiscountAmount(invoice.getDiscountAmount());
        response.setTotalAmount(invoice.getTotalAmount());
        response.setPaidAmount(invoice.getPaidAmount());
        response.setRemainingAmount(invoice.getRemainingAmount());
        response.setStatus(invoice.getStatus());
        response.setDueDate(invoice.getDueDate());
        long daysOverdue = 0;
        if (invoice.getStatus() != InvoiceStatus.DRAFT
                && invoice.getStatus() != InvoiceStatus.CANCELLED
                && invoice.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0
                && invoice.getDueDate().isBefore(LocalDate.now())) {
            daysOverdue = ChronoUnit.DAYS.between(invoice.getDueDate(), LocalDate.now());
        }
        response.setDaysOverdue(daysOverdue);
        response.setNotes(invoice.getNotes());
        response.setPrevElectricIndex(invoice.getPrevElectricIndex());
        response.setCurrElectricIndex(invoice.getCurrElectricIndex());
        response.setElectricUsage(invoice.getElectricUsage());
        response.setElectricUnitPrice(invoice.getElectricUnitPrice());
        response.setPrevWaterIndex(invoice.getPrevWaterIndex());
        response.setCurrWaterIndex(invoice.getCurrWaterIndex());
        response.setWaterUsage(invoice.getWaterUsage());
        response.setWaterUnitPrice(invoice.getWaterUnitPrice());
        response.setWaterBillingMode(invoice.getWaterBillingMode() != null ? invoice.getWaterBillingMode().name() : WaterBillingMode.METERED.name());
        response.setOccupantCount(invoice.getOccupantCount());
        response.setPublishedAt(invoice.getPublishedAt());
        response.setCancelledAt(invoice.getCancelledAt());
        response.setCancellationReason(invoice.getCancellationReason());
        response.setPaymentProofNote(invoice.getPaymentProofNote());
        response.setPaymentProofUrl(invoice.getPaymentProofUrl());
        response.setPaymentProofSubmittedAt(invoice.getPaymentProofSubmittedAt());

        if (utilityRecordRepository != null
                && (invoice.getUtilityRecordId() != null
                        || response.getElectricUsage() == null
                        || response.getWaterUsage() == null)) {
            Optional<UtilityRecord> utilityOpt = invoice.getUtilityRecordId() != null
                    ? utilityRecordRepository.findById(invoice.getUtilityRecordId())
                    : utilityRecordRepository.findByRoomIdAndBillingMonth(invoice.getRoom().getId(), invoice.getBillingMonth());
            utilityOpt.ifPresent(u -> {
                if (response.getPrevElectricIndex() == null) response.setPrevElectricIndex(u.getPrevElectricIndex());
                if (response.getCurrElectricIndex() == null) response.setCurrElectricIndex(u.getCurrElectricIndex());
                if (response.getElectricUsage() == null) response.setElectricUsage(u.getElectricUsage());
                if (response.getElectricUnitPrice() == null) response.setElectricUnitPrice(u.getElectricUnitPrice());
                if (response.getPrevWaterIndex() == null) response.setPrevWaterIndex(u.getPrevWaterIndex());
                if (response.getCurrWaterIndex() == null) response.setCurrWaterIndex(u.getCurrWaterIndex());
                if (response.getWaterUsage() == null) response.setWaterUsage(u.getWaterUsage());
                if (response.getWaterUnitPrice() == null) response.setWaterUnitPrice(u.getWaterUnitPrice());
            });
        }
        if (includePayments && invoice.getId() != null) {
            List<PaymentResponse> payments = paymentRepository.findByInvoiceIdOrderByPaidAtDesc(invoice.getId())
                    .stream()
                    .map(p -> new PaymentResponse(
                            p.getId(),
                            invoice.getId(),
                            p.getAmount(),
                            p.getPaymentMethod().name(),
                            p.getTransactionCode(),
                            p.getReceiptNote(),
                            p.getPaidAt(),
                            p.getCreatedAt()
                    ))
                    .collect(Collectors.toList());
            response.setPayments(payments);
        }
        response.setCreatedAt(invoice.getCreatedAt());
        response.setUpdatedAt(invoice.getUpdatedAt());
        return response;
    }
}
