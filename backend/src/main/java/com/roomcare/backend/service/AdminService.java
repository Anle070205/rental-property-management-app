package com.roomcare.backend.service;

import com.roomcare.backend.dto.admin.*;
import com.roomcare.backend.dto.user.ChangePasswordRequest;
import com.roomcare.backend.entity.*;
import com.roomcare.backend.repository.*;
import com.roomcare.backend.security.JwtUtil;
import com.roomcare.backend.util.PhoneNumberUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final DateTimeFormatter ACTIVITY_TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter ACTIVITY_DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final RoomRepository roomRepository;
    private final TenantRepository tenantRepository;
    private final ContractRepository contractRepository;
    private final IncidentRepository incidentRepository;
    private final AdminBroadcastRepository broadcastRepository;
    private final NotificationService notificationService;
    private final UserProfileService userProfileService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final SecureRandom secureRandom = new SecureRandom();

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard(User admin) {
        List<User> allUsers = userRepository.findAll();
        List<Property> allProps = propertyRepository.findAll();
        List<Room> allRooms = roomRepository.findAll();
        List<Incident> allIncidents = incidentRepository.findAll();

        long totalAccounts = allUsers.size();
        long landlordCount = allUsers.stream().filter(u -> u.getRole() == Role.LANDLORD).count();
        long tenantCount = allUsers.stream().filter(u -> u.getRole() == Role.TENANT).count();
        long adminCount = allUsers.stream().filter(u -> u.getRole() == Role.ADMIN).count();
        long activeAccounts = allUsers.stream().filter(u -> Boolean.TRUE.equals(u.getIsActive())).count();
        long lockedAccounts = totalAccounts - activeAccounts;
        double activePct = totalAccounts == 0 ? 0.0 : Math.round((activeAccounts * 1000.0) / totalAccounts) / 10.0;

        long occupiedRooms = allRooms.stream()
                .filter(r -> r.getStatus() == RoomStatus.OCCUPIED || r.getStatus() == RoomStatus.EXPIRING_SOON)
                .count();

        YearMonth nowYm = YearMonth.now();
        List<AdminDashboardResponse.MonthlyPoint> monthlyGrowth = new ArrayList<>();
        for (int i = 5; i >= 0; i--) {
            YearMonth ym = nowYm.minusMonths(i);
            LocalDateTime endOfMonth = ym.atEndOfMonth().atTime(23, 59, 59);
            long cumulative = allUsers.stream()
                    .filter(u -> u.getCreatedAt() == null || !u.getCreatedAt().isAfter(endOfMonth))
                    .count();
            monthlyGrowth.add(new AdminDashboardResponse.MonthlyPoint("T" + ym.getMonthValue(), cumulative));
        }

        List<AdminDashboardResponse.RecentActivity> activities = new ArrayList<>();
        allUsers.stream()
                .filter(u -> u.getRole() != Role.ADMIN)
                .sorted(Comparator.comparing(User::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(3)
                .forEach(u -> {
                    String roleLabel = u.getRole() == Role.LANDLORD ? "Chủ nhà mới đăng ký" : "Tài khoản người thuê mới";
                    String timeLabel = u.getCreatedAt() != null ? u.getCreatedAt().format(ACTIVITY_TIME_FMT) : "--:--";
                    String dateLabel = u.getCreatedAt() != null ? u.getCreatedAt().format(ACTIVITY_DATE_FMT) : "";
                    activities.add(new AdminDashboardResponse.RecentActivity(
                            timeLabel, roleLabel, u.getFullName() + " · " + dateLabel, "ACCOUNT", u.getId()
                    ));
                });

        allIncidents.stream()
                .sorted(Comparator.comparing(Incident::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(3)
                .forEach(inc -> {
                    String timeLabel = inc.getCreatedAt() != null ? inc.getCreatedAt().format(ACTIVITY_TIME_FMT) : "--:--";
                    String statusVi = switch (inc.getStatus()) {
                        case RECEIVED -> "Chờ xử lý";
                        case IN_PROGRESS -> "Đang xử lý";
                        case DONE -> "Đã xử lý";
                    };
                    String sender = inc.getTenant() != null ? inc.getTenant().getFullName() : "Người dùng";
                    activities.add(new AdminDashboardResponse.RecentActivity(
                            timeLabel, "Tiếp nhận báo cáo #BC" + String.format("%03d", inc.getId()),
                            sender + " · " + statusVi, "REPORT", inc.getId()
                    ));
                });

        return new AdminDashboardResponse(
                admin.getFullName(),
                totalAccounts,
                landlordCount,
                tenantCount,
                adminCount,
                allProps.size(),
                allRooms.size(),
                occupiedRooms,
                activeAccounts,
                lockedAccounts,
                activePct,
                monthlyGrowth,
                activities.stream().limit(5).toList()
        );
    }

    @Transactional(readOnly = true)
    public AdminAccountResponse.PageResult listAccounts(String roleFilter, Boolean isActiveFilter, String keyword, int page, int size) {
        List<User> allUsers = userRepository.findAll().stream()
                .sorted(Comparator.comparing(User::getId, Comparator.reverseOrder()))
                .toList();

        long allCount = allUsers.size();
        long adminCount = allUsers.stream().filter(u -> u.getRole() == Role.ADMIN).count();
        long landlordCount = allUsers.stream().filter(u -> u.getRole() == Role.LANDLORD).count();
        long tenantCount = allUsers.stream().filter(u -> u.getRole() == Role.TENANT).count();
        long activeCount = allUsers.stream().filter(u -> Boolean.TRUE.equals(u.getIsActive())).count();
        long lockedCount = allCount - activeCount;

        String kw = keyword != null ? keyword.trim().toLowerCase(Locale.ROOT) : "";
        Role parsedRole = parseOptionalRole(roleFilter);

        List<User> filtered = allUsers.stream()
                .filter(u -> parsedRole == null || u.getRole() == parsedRole)
                .filter(u -> isActiveFilter == null || Boolean.TRUE.equals(u.getIsActive()) == isActiveFilter)
                .filter(u -> kw.isEmpty()
                        || (u.getFullName() != null && u.getFullName().toLowerCase(Locale.ROOT).contains(kw))
                        || (u.getEmail() != null && u.getEmail().toLowerCase(Locale.ROOT).contains(kw))
                        || (u.getPhone() != null && u.getPhone().toLowerCase(Locale.ROOT).contains(kw))
                        || ("#tk" + u.getId()).contains(kw))
                .toList();

        int safeSize = Math.max(1, Math.min(size, 50));
        int safePage = Math.max(0, page);
        int totalElements = filtered.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalElements / safeSize));
        int fromIdx = Math.min(safePage * safeSize, totalElements);
        int toIdx = Math.min(fromIdx + safeSize, totalElements);

        List<AdminAccountResponse> items = filtered.subList(fromIdx, toIdx).stream()
                .map(u -> mapAccount(u, null))
                .toList();

        return new AdminAccountResponse.PageResult(
                items, safePage, safeSize, totalElements, totalPages,
                allCount, adminCount, landlordCount, tenantCount, activeCount, lockedCount
        );
    }

    @Transactional(readOnly = true)
    public AdminAccountResponse getAccountDetail(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));
        return mapAccount(user, null);
    }

    @Transactional
    public AdminAccountResponse createAccount(AdminCreateAccountRequest req) {
        Role role = parseRequiredRole(req.role());
        String email = req.email().trim().toLowerCase(Locale.ROOT);
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new RuntimeException("Email đã tồn tại trong hệ thống");
        }
        String normalizedPhone = normalizeOptionalPhone(req.phone());
        if (normalizedPhone != null && userRepository.existsByPhoneIn(PhoneNumberUtils.lookupVariants(normalizedPhone))) {
            throw new RuntimeException("Số điện thoại đã tồn tại trong hệ thống");
        }

        Tenant linkedTenantProfile = null;
        Room linkedRoom = null;
        if (role == Role.TENANT) {
            if (req.landlordId() == null || req.propertyId() == null) {
                throw new RuntimeException("Tài khoản Người thuê bắt buộc chọn Chủ nhà và Nhà trọ quản lý");
            }
            User landlord = userRepository.findById(req.landlordId())
                    .filter(u -> u.getRole() == Role.LANDLORD)
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy Chủ nhà quản lý"));
            Property property = propertyRepository.findById(req.propertyId())
                    .filter(p -> p.getLandlord().getId().equals(landlord.getId()))
                    .orElseThrow(() -> new RuntimeException("Nhà trọ không thuộc Chủ nhà đã chọn"));
            if (req.roomId() != null) {
                linkedRoom = roomRepository.findById(req.roomId())
                        .filter(r -> r.getProperty().getId().equals(property.getId()))
                        .orElseThrow(() -> new RuntimeException("Phòng không thuộc Nhà trọ đã chọn"));
            }
            linkedTenantProfile = new Tenant();
            linkedTenantProfile.setLandlord(landlord);
            linkedTenantProfile.setProperty(property);
            linkedTenantProfile.setFullName(req.fullName().trim());
            linkedTenantProfile.setEmail(email);
            linkedTenantProfile.setPhone(normalizedPhone);
        }

        String rawPassword = (req.password() != null && !req.password().isBlank())
                ? req.password()
                : generateRandomPassword();
        User user = new User();
        user.setFullName(req.fullName().trim());
        user.setEmail(email);
        user.setPhone(normalizedPhone);
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setIsActive(req.isActive() == null || req.isActive());
        user.setEmailVerified(true);
        user.setPhoneVerified(normalizedPhone != null);
        user.setEmailVerifiedAt(LocalDateTime.now());
        if (normalizedPhone != null) {
            user.setPhoneVerifiedAt(LocalDateTime.now());
        }
        user.setTokenVersion(0);
        User saved = userRepository.save(user);

        if (linkedTenantProfile != null) {
            linkedTenantProfile.setUser(saved);
            tenantRepository.save(linkedTenantProfile);
        }

        String note = (req.password() != null && !req.password().isBlank())
                ? "Tài khoản đã được tạo và kích hoạt thành công."
                : "Tài khoản đã được tạo với mật khẩu ngẫu nhiên bảo mật.";
        return mapAccount(saved, note);
    }

    @Transactional
    public AdminAccountResponse updateAccount(Long adminId, Long userId, AdminUpdateAccountRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));
        String cleanEmail = req.email().trim().toLowerCase(Locale.ROOT);
        userRepository.findByEmailIgnoreCase(cleanEmail).ifPresent(other -> {
            if (!other.getId().equals(userId)) {
                throw new RuntimeException("Email đã được sử dụng bởi tài khoản khác");
            }
        });
        String cleanPhone = normalizeOptionalPhone(req.phone());
        if (cleanPhone != null) {
            userRepository.findFirstByPhoneIn(PhoneNumberUtils.lookupVariants(cleanPhone)).ifPresent(other -> {
                if (!other.getId().equals(userId)) {
                    throw new RuntimeException("Số điện thoại đã được sử dụng bởi tài khoản khác");
                }
            });
        }

        if (req.role() != null && !req.role().isBlank()) {
            Role newRole = parseRequiredRole(req.role());
            if (newRole != user.getRole()) {
                if (tenantRepository.findByUserId(userId).isPresent()) {
                    throw new RuntimeException("Không thể thay đổi vai trò khi tài khoản đang liên kết thuê phòng");
                }
                if (adminId.equals(userId)) {
                    throw new RuntimeException("Không thể tự thay đổi vai trò tài khoản Admin đang đăng nhập");
                }
                user.setRole(newRole);
            }
        }

        if (req.isActive() != null && !req.isActive().equals(user.getIsActive())) {
            if (adminId.equals(userId) && !req.isActive()) {
                throw new RuntimeException("Không thể tự khóa tài khoản Admin đang đăng nhập");
            }
            user.setIsActive(req.isActive());
            if (!req.isActive()) {
                user.setTokenVersion((user.getTokenVersion() != null ? user.getTokenVersion() : 0) + 1);
            }
        }

        user.setFullName(req.fullName().trim());
        user.setEmail(cleanEmail);
        user.setPhone(cleanPhone);
        User saved = userRepository.save(user);
        return mapAccount(saved, "Đã cập nhật thông tin tài khoản.");
    }

    @Transactional
    public AdminAccountResponse lockAccount(Long adminId, Long userId) {
        if (adminId.equals(userId)) {
            throw new RuntimeException("Không thể tự khóa tài khoản Admin đang đăng nhập");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));
        user.setIsActive(false);
        user.setTokenVersion((user.getTokenVersion() != null ? user.getTokenVersion() : 0) + 1);
        User saved = userRepository.save(user);
        return mapAccount(saved, "Tài khoản đã bị khóa.");
    }

    @Transactional
    public AdminAccountResponse unlockAccount(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản"));
        user.setIsActive(true);
        User saved = userRepository.save(user);
        return mapAccount(saved, "Đã mở khóa tài khoản.");
    }

    @Transactional(readOnly = true)
    public AdminLandlordResponse.ListResult listLandlords(String keyword) {
        String kw = keyword != null ? keyword.trim().toLowerCase(Locale.ROOT) : "";
        List<User> landlords = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.LANDLORD)
                .filter(u -> kw.isEmpty()
                        || (u.getFullName() != null && u.getFullName().toLowerCase(Locale.ROOT).contains(kw))
                        || (u.getEmail() != null && u.getEmail().toLowerCase(Locale.ROOT).contains(kw))
                        || (u.getPhone() != null && u.getPhone().toLowerCase(Locale.ROOT).contains(kw)))
                .sorted(Comparator.comparing(User::getId, Comparator.reverseOrder()))
                .toList();

        List<Property> allProps = propertyRepository.findAll();
        List<Room> allRooms = roomRepository.findAll();
        List<Tenant> allTenants = tenantRepository.findAll();

        List<AdminLandlordResponse> items = landlords.stream()
                .map(l -> buildLandlordResponse(l, allProps, allRooms, allTenants, false))
                .toList();

        long active = landlords.stream().filter(u -> Boolean.TRUE.equals(u.getIsActive())).count();
        return new AdminLandlordResponse.ListResult(items, items.size(), active, items.size() - active);
    }

    @Transactional(readOnly = true)
    public AdminLandlordResponse getLandlordDetail(Long landlordId) {
        User landlord = userRepository.findById(landlordId)
                .filter(u -> u.getRole() == Role.LANDLORD)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Chủ nhà"));
        return buildLandlordResponse(
                landlord, propertyRepository.findAll(), roomRepository.findAll(), tenantRepository.findAll(), true
        );
    }

    @Transactional(readOnly = true)
    public AdminPropertyResponse.ListResult listProperties(String keyword) {
        String kw = keyword != null ? keyword.trim().toLowerCase(Locale.ROOT) : "";
        List<Property> props = propertyRepository.findAll().stream()
                .filter(p -> kw.isEmpty()
                        || (p.getName() != null && p.getName().toLowerCase(Locale.ROOT).contains(kw))
                        || (p.getAddress() != null && p.getAddress().toLowerCase(Locale.ROOT).contains(kw))
                        || (p.getLandlord() != null && p.getLandlord().getFullName().toLowerCase(Locale.ROOT).contains(kw)))
                .sorted(Comparator.comparing(Property::getId, Comparator.reverseOrder()))
                .toList();

        List<Room> allRooms = roomRepository.findAll();
        List<Tenant> allTenants = tenantRepository.findAll();
        List<Contract> allContracts = contractRepository.findAll();

        List<AdminPropertyResponse> items = props.stream()
                .map(p -> buildPropertyResponse(p, allRooms, allTenants, allContracts, false))
                .toList();

        long totalRooms = items.stream().mapToLong(AdminPropertyResponse::totalRooms).sum();
        long activeCount = items.stream().filter(i -> "Đang hoạt động".equals(i.statusLabel())).count();
        return new AdminPropertyResponse.ListResult(items, items.size(), totalRooms, activeCount, items.size() - activeCount);
    }

    @Transactional(readOnly = true)
    public AdminPropertyResponse getPropertyDetail(Long propertyId) {
        Property prop = propertyRepository.findById(propertyId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy Nhà trọ"));
        return buildPropertyResponse(
                prop, roomRepository.findAll(), tenantRepository.findAll(), contractRepository.findAll(), true
        );
    }

    @Transactional(readOnly = true)
    public AdminReportResponse.ListResult listReports(String keyword, String statusFilter) {
        String kw = keyword != null ? keyword.trim().toLowerCase(Locale.ROOT) : "";
        List<Incident> incidents = incidentRepository.findAll().stream()
                .sorted(Comparator.comparing(Incident::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();

        long totalCount = incidents.size();
        long pendingCount = incidents.stream().filter(i -> i.getStatus() == Incident.Status.RECEIVED).count();
        long inProgressCount = incidents.stream().filter(i -> i.getStatus() == Incident.Status.IN_PROGRESS).count();
        long doneCount = incidents.stream().filter(i -> i.getStatus() == Incident.Status.DONE).count();

        List<AdminReportResponse> items = incidents.stream()
                .map(this::mapReport)
                .filter(r -> statusFilter == null || statusFilter.isBlank() || r.status().equalsIgnoreCase(statusFilter.trim()))
                .filter(r -> kw.isEmpty()
                        || r.reportCode().toLowerCase(Locale.ROOT).contains(kw)
                        || r.title().toLowerCase(Locale.ROOT).contains(kw)
                        || r.senderName().toLowerCase(Locale.ROOT).contains(kw)
                        || r.description().toLowerCase(Locale.ROOT).contains(kw))
                .toList();

        return new AdminReportResponse.ListResult(items, totalCount, pendingCount, inProgressCount, doneCount);
    }

    @Transactional(readOnly = true)
    public AdminReportResponse getReportDetail(Long reportId) {
        Incident inc = incidentRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy báo cáo"));
        return mapReport(inc);
    }

    @Transactional
    public AdminReportResponse resolveReport(User admin, Long reportId, AdminResolveReportRequest req) {
        Incident inc = incidentRepository.findById(reportId)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy báo cáo"));

        Incident.Status targetStatus;
        try {
            targetStatus = Incident.Status.valueOf(req.status().trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new RuntimeException("Trạng thái xử lý không hợp lệ (IN_PROGRESS hoặc DONE)");
        }
        inc.setStatus(targetStatus);
        inc.setLandlordResponse(req.responseNote().trim());
        inc.setResolvedById(admin.getId());
        inc.setResolvedByName(admin.getFullName());
        inc.setResolvedAt(LocalDateTime.now());
        Incident saved = incidentRepository.save(inc);

        if (saved.getTenant() != null && saved.getTenant().getUser() != null) {
            notificationService.createNotification(
                    saved.getTenant().getUser(),
                    NotificationType.INCIDENT,
                    "Admin đã phản hồi báo cáo #BC" + String.format("%03d", saved.getId()),
                    req.responseNote().trim(),
                    saved.getId()
            );
        }
        return mapReport(saved);
    }

    @Transactional(readOnly = true)
    public AdminBroadcastResponse.AudienceCounts getAudienceCounts() {
        List<User> all = userRepository.findAll();
        long allActive = all.stream().filter(u -> Boolean.TRUE.equals(u.getIsActive())).count();
        long adminActive = all.stream().filter(u -> u.getRole() == Role.ADMIN && Boolean.TRUE.equals(u.getIsActive())).count();
        long landlordActive = all.stream().filter(u -> u.getRole() == Role.LANDLORD && Boolean.TRUE.equals(u.getIsActive())).count();
        long tenantActive = all.stream().filter(u -> u.getRole() == Role.TENANT && Boolean.TRUE.equals(u.getIsActive())).count();
        long locked = all.size() - allActive;
        return new AdminBroadcastResponse.AudienceCounts(allActive, adminActive, landlordActive, tenantActive, locked);
    }

    @Transactional(readOnly = true)
    public AdminBroadcastResponse.ListResult listBroadcasts(String keyword) {
        String kw = keyword != null ? keyword.trim().toLowerCase(Locale.ROOT) : "";
        List<AdminBroadcast> all = broadcastRepository.findAllByOrderByUpdatedAtDescIdDesc();
        long total = all.size();
        long sent = all.stream().filter(b -> b.getStatus() == AdminBroadcast.Status.SENT).count();
        long draft = total - sent;

        List<AdminBroadcastResponse> items = all.stream()
                .filter(b -> kw.isEmpty()
                        || b.getTitle().toLowerCase(Locale.ROOT).contains(kw)
                        || b.getContent().toLowerCase(Locale.ROOT).contains(kw)
                        || b.getBroadcastCode().toLowerCase(Locale.ROOT).contains(kw))
                .map(this::mapBroadcast)
                .toList();
        return new AdminBroadcastResponse.ListResult(items, total, sent, draft);
    }

    @Transactional(readOnly = true)
    public AdminBroadcastResponse getBroadcastDetail(Long id) {
        AdminBroadcast b = broadcastRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo"));
        return mapBroadcast(b);
    }

    @Transactional
    public AdminBroadcastResponse createBroadcast(User admin, AdminBroadcastRequest req, boolean sendNow) {
        AdminBroadcast b = new AdminBroadcast();
        b.setBroadcastCode("TB" + String.format("%03d", broadcastRepository.count() + 1) + "-" + (100 + secureRandom.nextInt(900)));
        b.setTitle(req.title().trim());
        b.setContent(req.content().trim());
        b.setTargetAudience(parseAudience(req.targetAudience()));
        b.setCreatedBy(admin);
        b.setCreatedByName(admin.getFullName() + " · ADMIN");
        b.setStatus(AdminBroadcast.Status.DRAFT);
        AdminBroadcast saved = broadcastRepository.save(b);
        return sendNow ? sendBroadcast(admin, saved.getId()) : mapBroadcast(saved);
    }

    @Transactional
    public AdminBroadcastResponse updateBroadcast(Long id, AdminBroadcastRequest req) {
        AdminBroadcast b = broadcastRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo"));
        if (b.getStatus() == AdminBroadcast.Status.SENT) {
            throw new RuntimeException("Thông báo đã gửi không thể chỉnh sửa");
        }
        b.setTitle(req.title().trim());
        b.setContent(req.content().trim());
        b.setTargetAudience(parseAudience(req.targetAudience()));
        return mapBroadcast(broadcastRepository.save(b));
    }

    @Transactional
    public AdminBroadcastResponse sendBroadcast(User admin, Long id) {
        AdminBroadcast b = broadcastRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy thông báo"));
        if (b.getStatus() == AdminBroadcast.Status.SENT) {
            throw new RuntimeException("Thông báo đã được phát hành trước đó");
        }
        List<User> recipients = userRepository.findAll().stream()
                .filter(u -> Boolean.TRUE.equals(u.getIsActive()))
                .filter(u -> switch (b.getTargetAudience()) {
                    case ALL -> true;
                    case ADMIN -> u.getRole() == Role.ADMIN;
                    case LANDLORD -> u.getRole() == Role.LANDLORD;
                    case TENANT -> u.getRole() == Role.TENANT;
                })
                .toList();

        for (User u : recipients) {
            notificationService.createNotification(u, NotificationType.SYSTEM, b.getTitle(), b.getContent(), b.getId());
        }
        b.setStatus(AdminBroadcast.Status.SENT);
        b.setRecipientCount(recipients.size());
        b.setSentAt(LocalDateTime.now());
        if (b.getCreatedBy() == null) {
            b.setCreatedBy(admin);
            b.setCreatedByName(admin.getFullName() + " · ADMIN");
        }
        return mapBroadcast(broadcastRepository.save(b));
    }

    @Transactional
    public AdminChangePasswordResponse changeAdminPassword(User admin, ChangePasswordRequest req) {
        userProfileService.changePassword(admin, req);
        User reloaded = userRepository.findById(admin.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy tài khoản Admin"));
        int ver = reloaded.getTokenVersion() != null ? reloaded.getTokenVersion() : 0;
        String freshToken = jwtUtil.generateToken(reloaded.getId(), reloaded.getPrimaryIdentifier(), reloaded.getRole().name(), ver);
        return new AdminChangePasswordResponse(freshToken, "Đã cập nhật mật khẩu. Các phiên đăng nhập khác đã được đăng xuất.");
    }

    private AdminAccountResponse mapAccount(User u, String noteMessage) {
        Optional<Tenant> tenantOpt = u.getRole() == Role.TENANT ? tenantRepository.findByUserId(u.getId()) : Optional.empty();
        Long landlordId = null;
        String landlordName = null;
        String landlordCode = null;
        Long propertyId = null;
        String propertyName = null;
        Long roomId = null;
        String roomCode = null;
        if (tenantOpt.isPresent()) {
            Tenant t = tenantOpt.get();
            if (t.getLandlord() != null) {
                landlordId = t.getLandlord().getId();
                landlordName = t.getLandlord().getFullName();
                landlordCode = String.format("LL%04d", landlordId);
            }
            if (t.getProperty() != null) {
                propertyId = t.getProperty().getId();
                propertyName = t.getProperty().getName();
            }
            Optional<Contract> activeC = contractRepository.findFirstByTenantIdAndStatusInOrderByActivatedAtDescCreatedAtDesc(
                    t.getId(), List.of(ContractStatus.ACTIVE, ContractStatus.SIGNED)
            );
            if (activeC.isPresent() && activeC.get().getRoom() != null) {
                roomId = activeC.get().getRoom().getId();
                roomCode = activeC.get().getRoom().getRoomCode();
            }
        }
        return new AdminAccountResponse(
                u.getId(),
                String.format("#TK%04d", u.getId()),
                u.getFullName(),
                u.getEmail(),
                u.getPhone(),
                u.getRole().name(),
                Boolean.TRUE.equals(u.getIsActive()),
                Boolean.TRUE.equals(u.getEmailVerified()),
                Boolean.TRUE.equals(u.getPhoneVerified()),
                u.getCreatedAt(),
                landlordId,
                landlordName,
                landlordCode,
                propertyId,
                propertyName,
                roomId,
                roomCode,
                tenantOpt.isPresent(),
                noteMessage
        );
    }

    private AdminLandlordResponse buildLandlordResponse(
            User l, List<Property> allProps, List<Room> allRooms, List<Tenant> allTenants, boolean includeDetails) {
        List<Property> owned = allProps.stream().filter(p -> p.getLandlord().getId().equals(l.getId())).toList();
        Set<Long> ownedIds = owned.stream().map(Property::getId).collect(Collectors.toSet());
        List<Room> ownedRooms = allRooms.stream().filter(r -> ownedIds.contains(r.getProperty().getId())).toList();
        List<Tenant> ownedTenants = allTenants.stream().filter(t -> t.getLandlord().getId().equals(l.getId())).toList();

        List<AdminLandlordResponse.PropertySummary> propSummaries = includeDetails
                ? owned.stream().map(p -> {
                    long rc = ownedRooms.stream().filter(r -> r.getProperty().getId().equals(p.getId())).count();
                    long occ = ownedRooms.stream().filter(r -> r.getProperty().getId().equals(p.getId())
                            && (r.getStatus() == RoomStatus.OCCUPIED || r.getStatus() == RoomStatus.EXPIRING_SOON)).count();
                    long tc = ownedTenants.stream().filter(t -> t.getProperty() != null && t.getProperty().getId().equals(p.getId())).count();
                    return new AdminLandlordResponse.PropertySummary(p.getId(), p.getName(), rc, occ, tc);
                }).toList()
                : List.of();

        List<AdminLandlordResponse.TenantSummary> tenantSummaries = includeDetails
                ? ownedTenants.stream().map(t -> new AdminLandlordResponse.TenantSummary(
                        t.getId(),
                        t.getUser() != null ? t.getUser().getId() : null,
                        t.getFullName(),
                        t.getProperty() != null ? t.getProperty().getName() : "",
                        ""
                )).toList()
                : List.of();

        String addr = owned.isEmpty() ? "" : owned.get(0).getAddress();
        return new AdminLandlordResponse(
                l.getId(),
                String.format("LL%04d", l.getId()),
                l.getFullName(),
                l.getEmail(),
                l.getPhone(),
                Boolean.TRUE.equals(l.getIsActive()),
                l.getCreatedAt(),
                addr,
                owned.size(),
                ownedRooms.size(),
                ownedTenants.size(),
                propSummaries,
                tenantSummaries
        );
    }

    private AdminPropertyResponse buildPropertyResponse(
            Property p, List<Room> allRooms, List<Tenant> allTenants, List<Contract> allContracts, boolean includeRooms) {
        List<Room> rooms = allRooms.stream().filter(r -> r.getProperty().getId().equals(p.getId())).toList();
        long occ = rooms.stream().filter(r -> r.getStatus() == RoomStatus.OCCUPIED || r.getStatus() == RoomStatus.EXPIRING_SOON).count();
        long vac = rooms.stream().filter(r -> r.getStatus() == RoomStatus.VACANT).count();
        long maint = rooms.stream().filter(r -> r.getStatus() == RoomStatus.NEEDS_ATTENTION).count();
        long tenants = allTenants.stream().filter(t -> t.getProperty() != null && t.getProperty().getId().equals(p.getId())).count();
        int occPct = rooms.isEmpty() ? 0 : (int) Math.round((occ * 100.0) / rooms.size());
        boolean landlordActive = p.getLandlord() != null && Boolean.TRUE.equals(p.getLandlord().getIsActive());

        List<AdminPropertyResponse.OccupiedRoomLookup> lookups = List.of();
        if (includeRooms) {
            lookups = rooms.stream()
                    .filter(r -> r.getStatus() == RoomStatus.OCCUPIED || r.getStatus() == RoomStatus.EXPIRING_SOON)
                    .map(r -> {
                        Optional<Contract> c = allContracts.stream()
                                .filter(ct -> ct.getRoom() != null && ct.getRoom().getId().equals(r.getId()) && ct.getStatus() == ContractStatus.ACTIVE)
                                .findFirst();
                        Long tUid = c.map(ct -> ct.getTenant().getUser() != null ? ct.getTenant().getUser().getId() : null).orElse(null);
                        String tName = c.map(ct -> ct.getTenant().getFullName()).orElse("Đang thuê");
                        return new AdminPropertyResponse.OccupiedRoomLookup(r.getId(), r.getRoomCode(), r.getFloor(), tUid, tName);
                    })
                    .toList();
        }
        return new AdminPropertyResponse(
                p.getId(),
                String.format("NT%04d", p.getId()),
                p.getName(),
                p.getAddress(),
                p.getLandlord() != null ? p.getLandlord().getId() : null,
                p.getLandlord() != null ? p.getLandlord().getFullName() : "",
                p.getLandlord() != null ? String.format("LL%04d", p.getLandlord().getId()) : "",
                landlordActive ? "Đang hoạt động" : "Tạm dừng",
                rooms.size(),
                occ,
                vac,
                maint,
                tenants,
                occPct,
                lookups
        );
    }

    private AdminReportResponse mapReport(Incident i) {
        Tenant t = i.getTenant();
        Room r = i.getRoom();
        return new AdminReportResponse(
                i.getId(),
                String.format("#BC%03d", i.getId()),
                i.getTitle(),
                i.getDescription(),
                i.getType().name(),
                i.getPriority().name(),
                i.getStatus().name(),
                t != null && t.getUser() != null ? t.getUser().getId() : null,
                t != null ? t.getFullName() : "Người dùng",
                "TENANT",
                t != null && t.getLandlord() != null ? t.getLandlord().getFullName() : "",
                t != null && t.getProperty() != null ? t.getProperty().getName() : "",
                r != null ? r.getRoomCode() : "",
                i.getLandlordResponse(),
                i.getResolvedByName(),
                i.getResolvedAt(),
                i.getCreatedAt()
        );
    }

    private AdminBroadcastResponse mapBroadcast(AdminBroadcast b) {
        return new AdminBroadcastResponse(
                b.getId(),
                "#" + b.getBroadcastCode(),
                b.getTitle(),
                b.getContent(),
                b.getTargetAudience().name(),
                b.getStatus().name(),
                b.getRecipientCount() != null ? b.getRecipientCount() : 0,
                b.getCreatedByName(),
                b.getSentAt(),
                b.getUpdatedAt()
        );
    }

    private Role parseOptionalRole(String raw) {
        if (raw == null || raw.isBlank() || "ALL".equalsIgnoreCase(raw.trim())) {
            return null;
        }
        return parseRequiredRole(raw);
    }

    private Role parseRequiredRole(String raw) {
        try {
            return Role.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new RuntimeException("Vai trò không hợp lệ (ADMIN, LANDLORD hoặc TENANT)");
        }
    }

    private AdminBroadcast.Audience parseAudience(String raw) {
        try {
            return AdminBroadcast.Audience.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (Exception ex) {
            throw new RuntimeException("Đối tượng nhận không hợp lệ (ALL, ADMIN, LANDLORD, TENANT)");
        }
    }

    private String normalizeOptionalPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        String trimmed = phone.trim();
        if (!PhoneNumberUtils.isValidVietnamMobile(trimmed)) {
            throw new RuntimeException("Số điện thoại Việt Nam không hợp lệ");
        }
        return PhoneNumberUtils.normalizeToE164(trimmed);
    }

    private String generateRandomPassword() {
        byte[] b = new byte[12];
        secureRandom.nextBytes(b);
        return HexFormat.of().formatHex(b);
    }
}
