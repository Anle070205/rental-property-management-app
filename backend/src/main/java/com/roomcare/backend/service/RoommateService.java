package com.roomcare.backend.service;

import com.roomcare.backend.dto.roommate.CreateRoommateRequest;
import com.roomcare.backend.dto.roommate.RoommateResponse;
import com.roomcare.backend.dto.roommate.UpdateRoommateRequest;
import com.roomcare.backend.entity.*;
import com.roomcare.backend.repository.*;
import com.roomcare.backend.util.PhoneNumberUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RoommateService {

    private final RoommateRepository roommateRepository;
    private final TenantRepository tenantRepository;
    private final RoomRepository roomRepository;
    private final ContractRepository contractRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<RoommateResponse> getRoommates(
            Long landlordId,
            Long tenantId,
            Long roomId,
            Long contractId) {
        List<Roommate> list = roommateRepository.findByLandlordIdOrderByCreatedAtDesc(landlordId);
        return list.stream()
                .filter(r -> tenantId == null || Objects.equals(r.getTenant().getId(), tenantId))
                .filter(r -> roomId == null || Objects.equals(r.getRoom().getId(), roomId))
                .filter(r -> contractId == null || (r.getContract() != null && Objects.equals(r.getContract().getId(), contractId)))
                .map(RoommateService::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public RoommateResponse addRoommate(Long landlordId, CreateRoommateRequest request) {
        Tenant tenant = tenantRepository.findById(request.getTenantId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách thuê đại diện"));
        if (!tenant.getLandlord().getId().equals(landlordId)) {
            throw new RuntimeException("Bạn không có quyền quản lý khách thuê này");
        }

        Room room = roomRepository.findById(request.getRoomId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phòng"));
        if (!room.getProperty().getLandlord().getId().equals(landlordId)) {
            throw new RuntimeException("Bạn không có quyền quản lý phòng này");
        }

        Contract contract = null;
        if (request.getContractId() != null) {
            contract = contractRepository.findById(request.getContractId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy hợp đồng"));
            if (!contract.getLandlord().getId().equals(landlordId)) {
                throw new RuntimeException("Bạn không có quyền truy cập hợp đồng này");
            }
            if (!Objects.equals(contract.getRoom().getId(), room.getId())
                    || !Objects.equals(contract.getTenant().getId(), tenant.getId())) {
                throw new RuntimeException("Hợp đồng không khớp với phòng hoặc khách thuê đại diện đã chọn");
            }
            if (contract.getStatus() == ContractStatus.TERMINATED || contract.getStatus() == ContractStatus.EXPIRED) {
                throw new RuntimeException("Không thể thêm người ở cùng vào hợp đồng đã kết thúc");
            }
        }

        long existingCount = contract != null
                ? roommateRepository.countByContractId(contract.getId())
                : roommateRepository.countByRoomId(room.getId());
        if (room.getCapacity() != null && (1 + existingCount + 1) > room.getCapacity()) {
            throw new RuntimeException("Phòng " + room.getRoomCode() + " đã đạt sức chứa tối đa (" + room.getCapacity() + " người)");
        }

        String normalizedIdentity = trimOrNull(request.getIdentityNumber());
        if (normalizedIdentity != null && !Boolean.TRUE.equals(request.getConsentConfirmed())) {
            throw new RuntimeException("Vui lòng xác nhận sự đồng ý của người ở cùng trước khi lưu số CMND/CCCD");
        }

        User landlord = userRepository.findById(landlordId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy chủ nhà"));

        Roommate roommate = new Roommate();
        roommate.setLandlord(landlord);
        roommate.setTenant(tenant);
        roommate.setRoom(room);
        roommate.setContract(contract);
        roommate.setFullName(request.getFullName().trim());
        roommate.setPhone(normalizeOptionalPhone(request.getPhone()));
        roommate.setIdentityNumber(normalizedIdentity);
        roommate.setDateOfBirth(request.getDateOfBirth());
        roommate.setRelationship(trimOrNull(request.getRelationship()));
        roommate.setMoveInDate(request.getMoveInDate());
        // Mac dinh nguoi o cung khong tu dong la dai dien hop dong hoac co tai khoan dang nhap
        roommate.setIsRepresentative(Boolean.TRUE.equals(request.getIsRepresentative()));
        roommate.setHasPortalAccess(Boolean.TRUE.equals(request.getHasPortalAccess()));
        roommate.setConsentConfirmed(Boolean.TRUE.equals(request.getConsentConfirmed()));
        roommate.setNotes(trimOrNull(request.getNotes()));

        return toResponse(roommateRepository.save(roommate));
    }

    @Transactional
    public RoommateResponse updateRoommate(Long landlordId, Long roommateId, UpdateRoommateRequest request) {
        Roommate roommate = roommateRepository.findById(roommateId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người ở cùng"));
        if (!roommate.getLandlord().getId().equals(landlordId)) {
            throw new RuntimeException("Bạn không có quyền chỉnh sửa người ở cùng này");
        }

        String normalizedIdentity = trimOrNull(request.getIdentityNumber());
        boolean consent = request.getConsentConfirmed() != null
                ? request.getConsentConfirmed()
                : Boolean.TRUE.equals(roommate.getConsentConfirmed());
        if (normalizedIdentity != null && !consent) {
            throw new RuntimeException("Vui lòng xác nhận sự đồng ý của người ở cùng trước khi lưu số CMND/CCCD");
        }

        roommate.setFullName(request.getFullName().trim());
        roommate.setPhone(normalizeOptionalPhone(request.getPhone()));
        roommate.setIdentityNumber(normalizedIdentity);
        roommate.setDateOfBirth(request.getDateOfBirth());
        roommate.setRelationship(trimOrNull(request.getRelationship()));
        roommate.setMoveInDate(request.getMoveInDate());
        if (request.getIsRepresentative() != null) {
            roommate.setIsRepresentative(request.getIsRepresentative());
        }
        if (request.getHasPortalAccess() != null) {
            roommate.setHasPortalAccess(request.getHasPortalAccess());
        }
        roommate.setConsentConfirmed(consent);
        roommate.setNotes(trimOrNull(request.getNotes()));

        return toResponse(roommateRepository.save(roommate));
    }

    @Transactional
    public void deleteRoommate(Long landlordId, Long roommateId) {
        Roommate roommate = roommateRepository.findById(roommateId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy người ở cùng"));
        if (!roommate.getLandlord().getId().equals(landlordId)) {
            throw new RuntimeException("Bạn không có quyền xóa người ở cùng này");
        }
        roommateRepository.delete(roommate);
    }

    public static RoommateResponse toResponse(Roommate r) {
        return new RoommateResponse(
                r.getId(),
                r.getTenant().getId(),
                r.getRoom().getId(),
                r.getRoom().getRoomCode(),
                r.getContract() != null ? r.getContract().getId() : null,
                r.getFullName(),
                r.getPhone(),
                maskIdentity(r.getIdentityNumber()),
                r.getDateOfBirth(),
                r.getRelationship(),
                r.getMoveInDate(),
                Boolean.TRUE.equals(r.getIsRepresentative()),
                Boolean.TRUE.equals(r.getHasPortalAccess()),
                Boolean.TRUE.equals(r.getConsentConfirmed()),
                r.getNotes(),
                r.getCreatedAt(),
                r.getUpdatedAt()
        );
    }

    private static String maskIdentity(String idNumber) {
        if (idNumber == null || idNumber.isBlank()) {
            return null;
        }
        if (idNumber.length() <= 4) {
            return "****";
        }
        return "*".repeat(idNumber.length() - 4) + idNumber.substring(idNumber.length() - 4);
    }

    private static String normalizeOptionalPhone(String rawPhone) {
        if (rawPhone == null || rawPhone.isBlank()) {
            return null;
        }
        String trimmed = rawPhone.trim();
        return PhoneNumberUtils.isValidVietnamMobile(trimmed)
                ? PhoneNumberUtils.normalizeToE164(trimmed)
                : trimmed;
    }

    private static String trimOrNull(String value) {
        return value != null && !value.isBlank() ? value.trim() : null;
    }
}
