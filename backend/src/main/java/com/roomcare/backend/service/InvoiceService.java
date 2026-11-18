package com.roomcare.backend.service;

import com.roomcare.backend.dto.invoice.BatchCreateInvoiceRequest;
import com.roomcare.backend.dto.invoice.BatchCreateInvoiceResult;
import com.roomcare.backend.dto.invoice.CreateInvoiceRequest;
import com.roomcare.backend.dto.invoice.InvoiceResponse;
import com.roomcare.backend.entity.Invoice;
import com.roomcare.backend.entity.InvoiceStatus;
import com.roomcare.backend.entity.Room;
import com.roomcare.backend.entity.RoomStatus;
import com.roomcare.backend.entity.Tenant;
import com.roomcare.backend.entity.User;
import com.roomcare.backend.repository.InvoiceRepository;
import com.roomcare.backend.repository.RoomRepository;
import com.roomcare.backend.repository.TenantRepository;
import com.roomcare.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final RoomRepository roomRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;

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
            if (invoice.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0
                    && invoice.getDueDate().isBefore(today)
                    && invoice.getStatus() != InvoiceStatus.OVERDUE) {
                invoice.setStatus(InvoiceStatus.OVERDUE);
                changed = true;
            }
        }
        if (changed) {
            invoiceRepository.saveAll(invoices);
        }

        return invoices.stream()
                .filter(inv -> status == null || inv.getStatus() == status)
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public InvoiceResponse createInvoice(Long landlordId, CreateInvoiceRequest request) {
        validateBillingMonth(request.getBillingMonth());

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));

        if (!room.getProperty().getLandlord().getId().equals(landlordId)) {
            throw new RuntimeException("Bạn không có quyền lập hóa đơn cho phòng này");
        }

        if (invoiceRepository.existsByRoomIdAndBillingMonth(room.getId(), request.getBillingMonth())) {
            throw new RuntimeException("Phòng " + room.getRoomCode() + " đã có hóa đơn cho kỳ " + request.getBillingMonth());
        }

        User landlord = userRepository.findById(landlordId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chủ nhà"));

        Tenant tenant = resolveTenantForRoom(landlordId, room, request.getTenantId());

        BigDecimal roomRent = request.getRoomRent() != null
                ? request.getRoomRent()
                : safeMoney(room.getRentPrice());
        BigDecimal electricity = safeMoney(request.getElectricityAmount());
        BigDecimal water = safeMoney(request.getWaterAmount());
        BigDecimal internet = safeMoney(request.getInternetAmount());
        BigDecimal service = safeMoney(request.getServiceAmount());
        BigDecimal otherFees = safeMoney(request.getOtherFees());
        BigDecimal discount = safeMoney(request.getDiscountAmount());

        BigDecimal previousDebt = request.getPreviousDebt() != null
                ? safeMoney(request.getPreviousDebt())
                : calculatePreviousDebt(room.getId(), request.getBillingMonth());

        Invoice invoice = buildInvoice(
                landlord, room, tenant, null,
                request.getBillingMonth(), request.getDueDate(),
                roomRent, electricity, water, internet, service,
                otherFees, previousDebt, discount, request.getNotes()
        );

        Invoice saved = invoiceRepository.save(invoice);
        return mapToResponse(saved);
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
            if (invoiceRepository.existsByRoomIdAndBillingMonth(room.getId(), request.getBillingMonth())) {
                skippedReasons.add("Phòng " + room.getRoomCode() + ": đã tồn tại hóa đơn kỳ " + request.getBillingMonth());
                continue;
            }

            Tenant tenant = resolveTenantForRoom(landlordId, room, null);
            BigDecimal roomRent = safeMoney(room.getRentPrice());
            BigDecimal electricity = BigDecimal.ZERO;
            BigDecimal water = BigDecimal.ZERO;
            BigDecimal internet = safeMoney(request.getDefaultInternetFee());
            BigDecimal service = safeMoney(request.getDefaultServiceFee());
            BigDecimal otherFees = safeMoney(request.getOtherFees());
            BigDecimal previousDebt = calculatePreviousDebt(room.getId(), request.getBillingMonth());

            Invoice invoice = buildInvoice(
                    landlord, room, tenant, null,
                    request.getBillingMonth(), request.getDueDate(),
                    roomRent, electricity, water, internet, service,
                    otherFees, previousDebt, BigDecimal.ZERO, request.getNotes()
            );

            Invoice saved = invoiceRepository.save(invoice);
            created.add(mapToResponse(saved));
        }

        return new BatchCreateInvoiceResult(
                created.size(),
                skippedReasons.size(),
                created,
                skippedReasons
        );
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

    protected Invoice buildInvoice(
            User landlord,
            Room room,
            Tenant tenant,
            Long utilityRecordId,
            String billingMonth,
            LocalDate dueDate,
            BigDecimal roomRent,
            BigDecimal electricity,
            BigDecimal water,
            BigDecimal internet,
            BigDecimal service,
            BigDecimal otherFees,
            BigDecimal previousDebt,
            BigDecimal discount,
            String notes) {

        BigDecimal subtotal = roomRent
                .add(electricity)
                .add(water)
                .add(internet)
                .add(service)
                .add(otherFees)
                .add(previousDebt);

        if (discount.compareTo(subtotal) > 0) {
            throw new RuntimeException("Số tiền giảm trừ không được lớn hơn tổng khoản phí");
        }

        BigDecimal totalAmount = subtotal.subtract(discount);

        Invoice invoice = new Invoice();
        invoice.setInvoiceCode(generateInvoiceCode(room, billingMonth));
        invoice.setLandlord(landlord);
        invoice.setProperty(room.getProperty());
        invoice.setRoom(room);
        invoice.setTenant(tenant);
        invoice.setUtilityRecordId(utilityRecordId);
        invoice.setBillingMonth(billingMonth);
        invoice.setRoomRent(roomRent);
        invoice.setElectricityAmount(electricity);
        invoice.setWaterAmount(water);
        invoice.setInternetAmount(internet);
        invoice.setServiceAmount(service);
        invoice.setOtherFees(otherFees);
        invoice.setPreviousDebt(previousDebt);
        invoice.setDiscountAmount(discount);
        invoice.setTotalAmount(totalAmount);
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setRemainingAmount(totalAmount);
        invoice.setDueDate(dueDate);
        invoice.setNotes(notes);
        invoice.setStatus(resolveStatus(totalAmount, BigDecimal.ZERO, dueDate));
        return invoice;
    }

    protected BigDecimal calculatePreviousDebt(Long roomId, String billingMonth) {
        List<Invoice> unpaidPrevious = invoiceRepository
                .findByRoomIdAndBillingMonthLessThanAndRemainingAmountGreaterThan(
                        roomId, billingMonth, BigDecimal.ZERO
                );
        return unpaidPrevious.stream()
                .map(Invoice::getRemainingAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    protected Tenant resolveTenantForRoom(Long landlordId, Room room, Long requestedTenantId) {
        if (requestedTenantId != null) {
            Tenant tenant = tenantRepository.findById(requestedTenantId)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy người thuê"));
            if (!tenant.getLandlord().getId().equals(landlordId)) {
                throw new RuntimeException("Người thuê không thuộc quyền quản lý của bạn");
            }
            return tenant;
        }

        List<Invoice> priorInvoices = invoiceRepository.findByRoomIdOrderByBillingMonthDesc(room.getId());
        for (Invoice prior : priorInvoices) {
            if (prior.getTenant() != null) {
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
        String baseCode = "INV-" + compactMonth + "-" + sanitizedRoom + "-R" + room.getId();
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

    protected InvoiceResponse mapToResponse(Invoice invoice) {
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
        response.setUtilityRecordId(invoice.getUtilityRecordId());
        response.setBillingMonth(invoice.getBillingMonth());
        response.setRoomRent(invoice.getRoomRent());
        response.setElectricityAmount(invoice.getElectricityAmount());
        response.setWaterAmount(invoice.getWaterAmount());
        response.setInternetAmount(invoice.getInternetAmount());
        response.setServiceAmount(invoice.getServiceAmount());
        response.setOtherFees(invoice.getOtherFees());
        response.setPreviousDebt(invoice.getPreviousDebt());
        response.setDiscountAmount(invoice.getDiscountAmount());
        response.setTotalAmount(invoice.getTotalAmount());
        response.setPaidAmount(invoice.getPaidAmount());
        response.setRemainingAmount(invoice.getRemainingAmount());
        response.setStatus(invoice.getStatus());
        response.setDueDate(invoice.getDueDate());
        long daysOverdue = 0;
        if (invoice.getRemainingAmount().compareTo(BigDecimal.ZERO) > 0
                && invoice.getDueDate().isBefore(LocalDate.now())) {
            daysOverdue = ChronoUnit.DAYS.between(invoice.getDueDate(), LocalDate.now());
        }
        response.setDaysOverdue(daysOverdue);
        response.setNotes(invoice.getNotes());
        response.setCreatedAt(invoice.getCreatedAt());
        response.setUpdatedAt(invoice.getUpdatedAt());
        return response;
    }
}
