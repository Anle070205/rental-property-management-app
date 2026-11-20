package com.roomcare.backend.service;

import com.roomcare.backend.dto.contract.*;
import com.roomcare.backend.dto.roommate.RoommateResponse;
import com.roomcare.backend.entity.*;
import com.roomcare.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository contractRepository;
    private final RoomRepository roomRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final RoommateRepository roommateRepository;
    private final NotificationService notificationService;

    @Transactional
    public List<ContractResponse> getContracts(
            Long landlordId,
            Long propertyId,
            Long roomId,
            Long tenantId,
            ContractStatus status) {
        List<Contract> contracts = contractRepository.findByLandlordWithFilters(
                landlordId, propertyId, roomId, tenantId, null);
        syncExpiredContracts(contracts);
        return contracts.stream()
                .filter(c -> status == null || c.getStatus() == status)
                .map(c -> mapToResponse(c, false))
                .collect(Collectors.toList());
    }

    @Transactional
    public ContractResponse getContractById(Long landlordId, Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng"));
        if (!contract.getLandlord().getId().equals(landlordId)) {
            throw new RuntimeException("Bạn không có quyền truy cập hợp đồng này");
        }
        syncExpiredContracts(List.of(contract));
        return mapToResponse(contract, true);
    }

    @Transactional
    public ContractResponse createContract(Long landlordId, CreateContractRequest request) {
        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));
        if (!room.getProperty().getLandlord().getId().equals(landlordId)) {
            throw new RuntimeException("Bạn không có quyền lập hợp đồng cho phòng này");
        }

        Tenant tenant = tenantRepository.findById(request.getTenantId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách thuê"));
        if (!tenant.getLandlord().getId().equals(landlordId)) {
            throw new RuntimeException("Khách thuê không thuộc quyền quản lý của bạn");
        }

        validateContractDatesAndOverlap(room, request.getStartDate(), request.getEndDate(), null);

        User landlord = userRepository.findById(landlordId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chủ nhà"));

        BigDecimal monthlyRent = request.getMonthlyRent() != null
                ? safeNonNegative(request.getMonthlyRent(), "Tiền thuê hàng tháng")
                : safeNonNegative(room.getRentPrice(), "Tiền thuê phòng");
        if (monthlyRent.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Tiền thuê hàng tháng phải lớn hơn 0");
        }
        BigDecimal depositAmount = request.getDepositAmount() != null
                ? safeNonNegative(request.getDepositAmount(), "Tiền đặt cọc")
                : safeNonNegative(room.getDeposit(), "Tiền đặt cọc");
        BigDecimal elecPrice = request.getElectricityUnitPrice() != null
                ? safeNonNegative(request.getElectricityUnitPrice(), "Đơn giá điện")
                : (room.getElectricityPrice() != null ? room.getElectricityPrice() : new BigDecimal("3500"));
        BigDecimal waterPrice = request.getWaterUnitPrice() != null
                ? safeNonNegative(request.getWaterUnitPrice(), "Đơn giá nước")
                : (room.getWaterPrice() != null ? room.getWaterPrice() : new BigDecimal("15000"));
        WaterBillingMode waterMode = request.getWaterBillingMode() != null
                ? request.getWaterBillingMode()
                : WaterBillingMode.METERED;
        BigDecimal internetFee = request.getInternetFee() != null
                ? safeNonNegative(request.getInternetFee(), "Phí Internet")
                : safeNonNegative(room.getInternetFee(), "Phí Internet");
        BigDecimal garbageFee = safeNonNegative(request.getGarbageFee(), "Phí rác");
        BigDecimal serviceFee = request.getServiceFee() != null
                ? safeNonNegative(request.getServiceFee(), "Phí dịch vụ")
                : safeNonNegative(room.getServiceFee(), "Phí dịch vụ");
        int payDay = request.getPaymentDayOfMonth() != null ? request.getPaymentDayOfMonth() : 5;

        Contract contract = new Contract();
        contract.setContractCode(generateContractCode(room));
        contract.setLandlord(landlord);
        contract.setProperty(room.getProperty());
        contract.setRoom(room);
        contract.setTenant(tenant);
        contract.setStartDate(request.getStartDate());
        contract.setEndDate(request.getEndDate());
        contract.setMonthlyRent(monthlyRent);
        contract.setDepositAmount(depositAmount);
        contract.setElectricityUnitPrice(elecPrice);
        contract.setWaterUnitPrice(waterPrice);
        contract.setWaterBillingMode(waterMode);
        contract.setInternetFee(internetFee);
        contract.setGarbageFee(garbageFee);
        contract.setServiceFee(serviceFee);
        contract.setPaymentDayOfMonth(payDay);
        contract.setDocumentUrl(trimOrNull(request.getDocumentUrl()));
        contract.setTerms(request.getTerms() != null && !request.getTerms().isBlank()
                ? request.getTerms().trim()
                : buildDefaultTerms(room, tenant, monthlyRent, depositAmount, payDay));
        // Tuyet doi khong tu dong coi hop dong la da ky hoac hieu luc khi moi tao
        contract.setStatus(ContractStatus.DRAFT);

        Contract saved = contractRepository.save(contract);
        return mapToResponse(saved, true);
    }

    @Transactional
    public ContractResponse updateDraftContract(Long landlordId, Long contractId, UpdateContractRequest request) {
        Contract contract = findOwnedContract(landlordId, contractId);
        if (contract.getStatus() != ContractStatus.DRAFT) {
            throw new RuntimeException("Chỉ được chỉnh sửa hợp đồng khi đang ở trạng thái Nháp (DRAFT)");
        }

        validateContractDatesAndOverlap(contract.getRoom(), request.getStartDate(), request.getEndDate(), contractId);

        BigDecimal monthlyRent = safeNonNegative(request.getMonthlyRent(), "Tiền thuê hàng tháng");
        if (monthlyRent.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Tiền thuê hàng tháng phải lớn hơn 0");
        }

        contract.setStartDate(request.getStartDate());
        contract.setEndDate(request.getEndDate());
        contract.setMonthlyRent(monthlyRent);
        if (request.getDepositAmount() != null) {
            contract.setDepositAmount(safeNonNegative(request.getDepositAmount(), "Tiền đặt cọc"));
        }
        if (request.getElectricityUnitPrice() != null) {
            contract.setElectricityUnitPrice(safeNonNegative(request.getElectricityUnitPrice(), "Đơn giá điện"));
        }
        if (request.getWaterUnitPrice() != null) {
            contract.setWaterUnitPrice(safeNonNegative(request.getWaterUnitPrice(), "Đơn giá nước"));
        }
        if (request.getWaterBillingMode() != null) {
            contract.setWaterBillingMode(request.getWaterBillingMode());
        }
        if (request.getInternetFee() != null) {
            contract.setInternetFee(safeNonNegative(request.getInternetFee(), "Phí Internet"));
        }
        if (request.getGarbageFee() != null) {
            contract.setGarbageFee(safeNonNegative(request.getGarbageFee(), "Phí rác"));
        }
        if (request.getServiceFee() != null) {
            contract.setServiceFee(safeNonNegative(request.getServiceFee(), "Phí dịch vụ"));
        }
        if (request.getPaymentDayOfMonth() != null) {
            contract.setPaymentDayOfMonth(request.getPaymentDayOfMonth());
        }
        if (request.getTerms() != null) {
            contract.setTerms(request.getTerms().trim());
        }
        if (request.getDocumentUrl() != null) {
            contract.setDocumentUrl(trimOrNull(request.getDocumentUrl()));
        }

        return mapToResponse(contractRepository.save(contract), true);
    }

    @Transactional
    public ContractResponse signContract(Long landlordId, Long contractId, SignContractRequest request) {
        Contract contract = findOwnedContract(landlordId, contractId);
        if (contract.getStatus() != ContractStatus.DRAFT) {
            throw new RuntimeException("Chỉ có thể xác nhận ký hợp đồng đang ở trạng thái Nháp");
        }
        if (contract.getEndDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Không thể ký hợp đồng đã quá ngày kết thúc");
        }

        validateContractDatesAndOverlap(contract.getRoom(), contract.getStartDate(), contract.getEndDate(), contractId);

        contract.setStatus(ContractStatus.SIGNED);
        contract.setSignedAt(LocalDateTime.now());
        if (request != null && request.getDocumentUrl() != null && !request.getDocumentUrl().isBlank()) {
            contract.setDocumentUrl(request.getDocumentUrl().trim());
        }
        Contract saved = contractRepository.save(contract);

        if (notificationService != null && saved.getTenant().getUser() != null) {
            notificationService.createNotification(
                    saved.getTenant().getUser(),
                    NotificationType.CONTRACT,
                    "Hợp đồng đã được ký",
                    "Hợp đồng " + saved.getContractCode() + " cho phòng " + saved.getRoom().getRoomCode() + " đã được xác nhận ký.",
                    saved.getId()
            );
        }
        return mapToResponse(saved, true);
    }

    @Transactional
    public ContractResponse activateContract(Long landlordId, Long contractId) {
        Contract contract = findOwnedContract(landlordId, contractId);
        if (contract.getStatus() != ContractStatus.SIGNED) {
            throw new RuntimeException("Hợp đồng phải được ký (SIGNED) trước khi kích hoạt hiệu lực");
        }
        if (contract.getEndDate().isBefore(LocalDate.now())) {
            throw new RuntimeException("Hợp đồng đã hết hạn, vui lòng tạo hoặc điều chỉnh thời hạn mới");
        }

        List<Contract> existingActive = contractRepository.findOverlappingContracts(
                contract.getRoom().getId(),
                List.of(ContractStatus.ACTIVE),
                LocalDate.of(1900, 1, 1),
                LocalDate.of(2999, 12, 31),
                contractId
        );
        if (!existingActive.isEmpty()) {
            throw new RuntimeException("Phòng " + contract.getRoom().getRoomCode() + " đang có hợp đồng hiệu lực khác. Vui lòng kết thúc hợp đồng cũ trước.");
        }
        validateContractDatesAndOverlap(contract.getRoom(), contract.getStartDate(), contract.getEndDate(), contractId);

        contract.setStatus(ContractStatus.ACTIVE);
        contract.setActivatedAt(LocalDateTime.now());
        Contract saved = contractRepository.save(contract);

        Room room = saved.getRoom();
        room.setStatus(RoomStatus.OCCUPIED);
        roomRepository.save(room);

        if (notificationService != null && saved.getTenant().getUser() != null) {
            notificationService.createNotification(
                    saved.getTenant().getUser(),
                    NotificationType.CONTRACT,
                    "Hợp đồng chính thức có hiệu lực",
                    "Hợp đồng " + saved.getContractCode() + " phòng " + room.getRoomCode() + " đã có hiệu lực.",
                    saved.getId()
            );
        }
        return mapToResponse(saved, true);
    }

    @Transactional
    public ContractResponse terminateContract(Long landlordId, Long contractId, TerminateContractRequest request) {
        Contract contract = findOwnedContract(landlordId, contractId);
        if (contract.getStatus() == ContractStatus.TERMINATED) {
            throw new RuntimeException("Hợp đồng này đã kết thúc trước đó");
        }
        contract.setStatus(ContractStatus.TERMINATED);
        contract.setTerminatedAt(LocalDateTime.now());
        contract.setTerminationReason(request != null ? trimOrNull(request.getTerminationReason()) : null);
        Contract saved = contractRepository.save(contract);

        Room room = saved.getRoom();
        boolean hasOtherActive = contractRepository.findByRoomIdOrderByStartDateDescCreatedAtDesc(room.getId())
                .stream()
                .anyMatch(c -> !Objects.equals(c.getId(), saved.getId()) && c.getStatus() == ContractStatus.ACTIVE);
        if (!hasOtherActive && room.getStatus() == RoomStatus.OCCUPIED) {
            room.setStatus(RoomStatus.VACANT);
            roomRepository.save(room);
        }

        if (notificationService != null && saved.getTenant().getUser() != null) {
            notificationService.createNotification(
                    saved.getTenant().getUser(),
                    NotificationType.CONTRACT,
                    "Hợp đồng đã chấm dứt",
                    "Hợp đồng " + saved.getContractCode() + " phòng " + room.getRoomCode() + " đã kết thúc.",
                    saved.getId()
            );
        }
        return mapToResponse(saved, true);
    }

    private void syncExpiredContracts(List<Contract> contracts) {
        LocalDate today = LocalDate.now();
        List<Contract> expired = contracts.stream()
                .filter(c -> c.getStatus() == ContractStatus.ACTIVE && c.getEndDate().isBefore(today))
                .peek(c -> c.setStatus(ContractStatus.EXPIRED))
                .collect(Collectors.toList());
        if (!expired.isEmpty()) {
            contractRepository.saveAll(expired);
        }
    }

    private void validateContractDatesAndOverlap(Room room, LocalDate startDate, LocalDate endDate, Long excludeContractId) {
        if (startDate == null || endDate == null || !endDate.isAfter(startDate)) {
            throw new RuntimeException("Ngày kết thúc hợp đồng phải sau ngày bắt đầu");
        }
        List<Contract> overlaps = contractRepository.findOverlappingContracts(
                room.getId(),
                List.of(ContractStatus.SIGNED, ContractStatus.ACTIVE),
                startDate,
                endDate,
                excludeContractId
        );
        if (!overlaps.isEmpty()) {
            Contract conflict = overlaps.get(0);
            throw new RuntimeException("Phòng " + room.getRoomCode() + " đã có hợp đồng "
                    + conflict.getContractCode() + " (" + conflict.getStartDate() + " → " + conflict.getEndDate()
                    + ") trùng khoảng thời gian này");
        }
    }

    private Contract findOwnedContract(Long landlordId, Long contractId) {
        Contract contract = contractRepository.findById(contractId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng"));
        if (!contract.getLandlord().getId().equals(landlordId)) {
            throw new RuntimeException("Bạn không có quyền thao tác hợp đồng này");
        }
        return contract;
    }

    public ContractResponse mapToResponse(Contract contract, boolean includeRoommates) {
        ContractResponse res = new ContractResponse();
        res.setId(contract.getId());
        res.setContractCode(contract.getContractCode());
        res.setLandlordId(contract.getLandlord().getId());
        res.setLandlordName(contract.getLandlord().getFullName());
        res.setLandlordPhone(contract.getLandlord().getPhone());
        res.setPropertyId(contract.getProperty().getId());
        res.setPropertyName(contract.getProperty().getName());
        res.setPropertyAddress(contract.getProperty().getAddress());
        res.setRoomId(contract.getRoom().getId());
        res.setRoomCode(contract.getRoom().getRoomCode());

        Tenant tenant = contract.getTenant();
        res.setTenantId(tenant.getId());
        res.setTenantName(tenant.getFullName());
        res.setTenantPhone(tenant.getPhone());
        res.setTenantEmail(tenant.getEmail());
        res.setTenantUserId(tenant.getUser() != null ? tenant.getUser().getId() : null);
        res.setTenantAccountStatus(resolveAccountStatus(tenant));

        res.setStartDate(contract.getStartDate());
        res.setEndDate(contract.getEndDate());
        res.setMonthlyRent(contract.getMonthlyRent());
        res.setDepositAmount(contract.getDepositAmount());
        res.setElectricityUnitPrice(contract.getElectricityUnitPrice());
        res.setWaterUnitPrice(contract.getWaterUnitPrice());
        res.setWaterBillingMode(contract.getWaterBillingMode());
        res.setInternetFee(contract.getInternetFee());
        res.setGarbageFee(contract.getGarbageFee());
        res.setServiceFee(contract.getServiceFee());
        res.setPaymentDayOfMonth(contract.getPaymentDayOfMonth());
        res.setTerms(contract.getTerms());
        res.setDocumentUrl(contract.getDocumentUrl());
        res.setStatus(contract.getStatus());
        res.setSignedAt(contract.getSignedAt());
        res.setActivatedAt(contract.getActivatedAt());
        res.setTerminatedAt(contract.getTerminatedAt());
        res.setTerminationReason(contract.getTerminationReason());

        List<Roommate> rmList = roommateRepository != null
                ? roommateRepository.findByContractIdOrderByCreatedAtAsc(contract.getId())
                : List.of();
        if (rmList.isEmpty() && roommateRepository != null) {
            rmList = roommateRepository.findByRoomIdOrderByCreatedAtAsc(contract.getRoom().getId())
                    .stream()
                    .filter(r -> Objects.equals(r.getTenant().getId(), tenant.getId()))
                    .collect(Collectors.toList());
        }
        res.setOccupantCount(1 + rmList.size());
        if (includeRoommates) {
            res.setRoommates(rmList.stream().map(RoommateService::toResponse).collect(Collectors.toList()));
        }
        res.setCreatedAt(contract.getCreatedAt());
        res.setUpdatedAt(contract.getUpdatedAt());
        return res;
    }

    public static String resolveAccountStatus(Tenant tenant) {
        if (tenant == null || tenant.getUser() == null) {
            return "NONE";
        }
        return tenant.getUser().isAccountVerified() ? "ACTIVE" : "PENDING_ACTIVATION";
    }

    private String generateContractCode(Room room) {
        String month = YearMonth.now().toString().replace("-", "");
        String cleanRoom = room.getRoomCode().replaceAll("[^A-Za-z0-9]", "").toUpperCase();
        String suffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        String code = "HD-" + month + "-" + cleanRoom + "-" + suffix;
        return code.length() > 50 ? code.substring(0, 50) : code;
    }

    private String buildDefaultTerms(Room room, Tenant tenant, BigDecimal rent, BigDecimal deposit, int payDay) {
        return "Hợp đồng thuê phòng " + room.getRoomCode() + " tại " + room.getProperty().getName()
                + " giữa bên cho thuê và khách thuê đại diện " + tenant.getFullName()
                + ". Tiền thuê hằng tháng " + rent.stripTrailingZeros().toPlainString() + " đ, tiền cọc "
                + deposit.stripTrailingZeros().toPlainString() + " đ. Thanh toán định kỳ trước ngày "
                + payDay + " hằng tháng. Giữ gìn tài sản chung và tuân thủ nội quy khu trọ.";
    }

    private BigDecimal safeNonNegative(BigDecimal value, String label) {
        if (value == null) return BigDecimal.ZERO;
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new RuntimeException(label + " không được âm");
        }
        return value;
    }

    private String trimOrNull(String value) {
        return value != null && !value.isBlank() ? value.trim() : null;
    }
}
