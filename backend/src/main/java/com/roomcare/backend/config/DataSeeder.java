package com.roomcare.backend.config;

import com.roomcare.backend.entity.*;
import com.roomcare.backend.dto.incident.CreateIncidentRequest;
import com.roomcare.backend.repository.*;
import com.roomcare.backend.service.IncidentService;
import com.roomcare.backend.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Seed data cho môi trường development
 * Chỉ chạy khi spring.profiles.active=dev
 */
@Configuration
@RequiredArgsConstructor
@Profile("dev")
public class DataSeeder {
    
    private final UserRepository userRepository;
    private final PropertyRepository propertyRepository;
    private final RoomRepository roomRepository;
    private final TenantRepository tenantRepository;
    private final UtilityRecordRepository utilityRecordRepository;
    private final InvoiceRepository invoiceRepository;
    private final PaymentRepository paymentRepository;
    private final IncidentRepository incidentRepository;
    private final PasswordEncoder passwordEncoder;
    private final IncidentService incidentService;
    private final NotificationService notificationService;
    
    @Bean
    public CommandLineRunner seedData() {
        return args -> {
            // Kiểm tra nếu chưa có users/properties/rooms/tenants thì seed Phase 1 + 2
            if (userRepository.count() == 0) {
                seedBaseData();
            } else {
                System.out.println("Base data already exists. Verifying sample credentials and encoding...");
                repairSampleDataIfNeeded();
            }

            // Seed dữ liệu Phase 4 (Member 2) nếu chưa có hóa đơn
            if (invoiceRepository.count() == 0) {
                seedFinanceAndHealthData();
            }

            // Seed sự cố & thông báo mẫu nếu bảng incidents đang trống
            if (incidentRepository.count() == 0) {
                seedSampleIncidentsAndNotifications();
            }
        };
    }

    private void repairSampleDataIfNeeded() {
        Map<String, String> expectedNames = Map.of(
                "landlord1@example.com", "Nguyễn Văn A",
                "landlord2@example.com", "Trần Thị B",
                "tenant1@example.com", "Lê Văn C"
        );
        expectedNames.forEach((email, fullName) -> userRepository.findByEmail(email).ifPresent(user -> {
            boolean dirty = false;
            if (!passwordEncoder.matches("123456", user.getPassword())) {
                user.setPassword(passwordEncoder.encode("123456"));
                dirty = true;
            }
            if (user.getFullName() == null || user.getFullName().contains("?")) {
                user.setFullName(fullName);
                dirty = true;
            }
            if (dirty) {
                userRepository.save(user);
            }
        }));

        List<Property> properties = propertyRepository.findAll();
        for (Property p : properties) {
            String fixed = null;
            if (p.getName() != null && p.getName().contains("Ho??ng Anh")) fixed = "Nhà trọ Hoàng Anh";
            else if (p.getName() != null && p.getName().contains("Minh Khang")) fixed = "Nhà trọ Minh Khang";
            else if (p.getName() != null && p.getName().contains("B??nh An")) fixed = "Nhà trọ Bình An";
            if (fixed != null) {
                p.setName(fixed);
                propertyRepository.save(p);
            }
        }

        List<Tenant> tenants = tenantRepository.findAll();
        for (Tenant t : tenants) {
            if ("0923456789".equals(t.getPhone()) && t.getFullName().contains("?")) {
                t.setFullName("Lê Văn C");
                t.setAddress("Bình Dương");
                tenantRepository.save(t);
            } else if ("0934567890".equals(t.getPhone()) && t.getFullName().contains("?")) {
                t.setFullName("Phạm Thị D");
                tenantRepository.save(t);
            } else if ("0945678901".equals(t.getPhone()) && t.getFullName().contains("?")) {
                t.setFullName("Hoàng Văn E");
                t.setAddress("Đồng Nai");
                tenantRepository.save(t);
            }
        }
    }

    private void seedSampleIncidentsAndNotifications() {
        User tenant1 = userRepository.findByEmail("tenant1@example.com").orElse(null);
        if (tenant1 == null || tenantRepository.findByUserId(tenant1.getId()).isEmpty()) {
            return;
        }
        var waterIncident = incidentService.create(tenant1, new CreateIncidentRequest(
                Incident.Type.WATER, "Vòi nước bị rò rỉ", "Vòi nước ở bồn rửa bị nhỏ giọt liên tục.",
                Incident.Priority.MEDIUM, null));
        incidentService.create(tenant1, new CreateIncidentRequest(
                Incident.Type.ELECTRIC, "Đèn hành lang chập chờn", "Đèn hành lang nhấp nháy vào buổi tối.",
                Incident.Priority.HIGH, null));
        notificationService.createNotification(tenant1, NotificationType.INCIDENT,
                "Đã tiếp nhận sự cố", "Chủ nhà đã nhận báo cáo về vòi nước.", waterIncident.id());
        notificationService.createNotification(tenant1, NotificationType.SYSTEM,
                "Chào mừng đến RoomCare", "Bạn có thể theo dõi hóa đơn, thanh toán QR và báo sự cố trên ứng dụng.", null);
    }

    private void seedBaseData() {
            System.out.println("Seeding development data...");
            
            // 1. Tạo Landlord users
            User landlord1 = new User();
            landlord1.setFullName("Nguyễn Văn A");
            landlord1.setEmail("landlord1@example.com");
            landlord1.setPhone("0901234567");
            landlord1.setPassword(passwordEncoder.encode("123456"));
            landlord1.setRole(Role.LANDLORD);
            landlord1 = userRepository.save(landlord1);
            
            User landlord2 = new User();
            landlord2.setFullName("Trần Thị B");
            landlord2.setEmail("landlord2@example.com");
            landlord2.setPhone("0912345678");
            landlord2.setPassword(passwordEncoder.encode("123456"));
            landlord2.setRole(Role.LANDLORD);
            landlord2 = userRepository.save(landlord2);
            
            // 2. Tạo Tenant users
            User tenant1 = new User();
            tenant1.setFullName("Lê Văn C");
            tenant1.setEmail("tenant1@example.com");
            tenant1.setPhone("0923456789");
            tenant1.setPassword(passwordEncoder.encode("123456"));
            tenant1.setRole(Role.TENANT);
            tenant1 = userRepository.save(tenant1);
            
            // 3. Tạo Properties
            Property property1 = new Property();
            property1.setLandlord(landlord1);
            property1.setName("Nhà trọ Hoàng Anh");
            property1.setAddress("123 Nguyễn Trãi, Quận 5, TP.HCM");
            property1.setDescription("Nhà trọ cao cấp, gần trường đại học");
            property1 = propertyRepository.save(property1);
            
            Property property2 = new Property();
            property2.setLandlord(landlord1);
            property2.setName("Nhà trọ Minh Khang");
            property2.setAddress("456 Lê Văn Sỹ, Quận 3, TP.HCM");
            property2.setDescription("Nhà trọ giá rẻ, an ninh tốt");
            property2 = propertyRepository.save(property2);
            
            Property property3 = new Property();
            property3.setLandlord(landlord2);
            property3.setName("Nhà trọ Bình An");
            property3.setAddress("789 Võ Văn Tần, Quận 3, TP.HCM");
            property3.setDescription("Nhà trọ mới xây, tiện nghi đầy đủ");
            property3 = propertyRepository.save(property3);
            
            // 4. Tạo Rooms cho property1
            Room room101 = new Room();
            room101.setProperty(property1);
            room101.setRoomCode("101");
            room101.setFloor(1);
            room101.setArea(new BigDecimal("20.00"));
            room101.setRentPrice(new BigDecimal("2500000"));
            room101.setDeposit(new BigDecimal("2500000"));
            room101.setCapacity(2);
            room101.setAmenities("Điều hòa, Nóng lạnh, Giường, Tủ");
            room101.setDescription("Phòng đẹp, thoáng mát");
            room101.setStatus(RoomStatus.VACANT);
            roomRepository.save(room101);
            
            Room room102 = new Room();
            room102.setProperty(property1);
            room102.setRoomCode("102");
            room102.setFloor(1);
            room102.setArea(new BigDecimal("25.00"));
            room102.setRentPrice(new BigDecimal("3000000"));
            room102.setDeposit(new BigDecimal("3000000"));
            room102.setCapacity(3);
            room102.setAmenities("Điều hòa, Nóng lạnh, Giường, Tủ, Ban công");
            room102.setDescription("Phòng rộng, có ban công");
            room102.setStatus(RoomStatus.OCCUPIED);
            roomRepository.save(room102);
            
            Room room201 = new Room();
            room201.setProperty(property1);
            room201.setRoomCode("201");
            room201.setFloor(2);
            room201.setArea(new BigDecimal("22.00"));
            room201.setRentPrice(new BigDecimal("2700000"));
            room201.setDeposit(new BigDecimal("2700000"));
            room201.setCapacity(2);
            room201.setAmenities("Điều hòa, Nóng lạnh, Giường");
            room201.setDescription("Phòng tầng 2, view đẹp");
            room201.setStatus(RoomStatus.EXPIRING_SOON);
            roomRepository.save(room201);
            
            // 5. Tạo Rooms cho property2
            Room room1A = new Room();
            room1A.setProperty(property2);
            room1A.setRoomCode("1A");
            room1A.setFloor(1);
            room1A.setArea(new BigDecimal("18.00"));
            room1A.setRentPrice(new BigDecimal("2000000"));
            room1A.setDeposit(new BigDecimal("2000000"));
            room1A.setCapacity(2);
            room1A.setAmenities("Điều hòa, Nóng lạnh");
            room1A.setDescription("Phòng giá rẻ");
            room1A.setStatus(RoomStatus.VACANT);
            roomRepository.save(room1A);
            
            Room room2B = new Room();
            room2B.setProperty(property2);
            room2B.setRoomCode("2B");
            room2B.setFloor(2);
            room2B.setArea(new BigDecimal("20.00"));
            room2B.setRentPrice(new BigDecimal("2300000"));
            room2B.setDeposit(new BigDecimal("2300000"));
            room2B.setCapacity(2);
            room2B.setAmenities("Điều hòa, Nóng lạnh, Giường, Tủ");
            room2B.setDescription("Phòng đẹp, tầng 2");
            room2B.setStatus(RoomStatus.NEEDS_ATTENTION);
            roomRepository.save(room2B);
            
            // 6. Tạo Rooms cho property3
            Room roomA1 = new Room();
            roomA1.setProperty(property3);
            roomA1.setRoomCode("A1");
            roomA1.setFloor(1);
            roomA1.setArea(new BigDecimal("30.00"));
            roomA1.setRentPrice(new BigDecimal("4000000"));
            roomA1.setDeposit(new BigDecimal("4000000"));
            roomA1.setCapacity(4);
            roomA1.setAmenities("Điều hòa, Nóng lạnh, Giường, Tủ, Bếp, Ban công");
            roomA1.setDescription("Phòng cao cấp, rộng rãi");
            roomA1.setStatus(RoomStatus.VACANT);
            roomRepository.save(roomA1);
            
            // 7. Tạo Tenants
            Tenant tenant1Obj = new Tenant();
            tenant1Obj.setLandlord(landlord1);
            tenant1Obj.setProperty(property1);
            tenant1Obj.setUser(tenant1);
            tenant1Obj.setFullName("Lê Văn C");
            tenant1Obj.setPhone("0923456789");
            tenant1Obj.setEmail("tenant1@example.com");
            tenant1Obj.setIdentityNumber("079012345678");
            tenant1Obj.setDateOfBirth(LocalDate.of(1995, 5, 15));
            tenant1Obj.setAddress("Bình Dương");
            tenant1Obj.setEmergencyContact("0987654321");
            tenantRepository.save(tenant1Obj);
            var waterIncident = incidentService.create(tenant1, new CreateIncidentRequest(
                    Incident.Type.WATER, "Vòi nước bị rò rỉ", "Vòi nước ở bồn rửa bị nhỏ giọt liên tục.",
                    Incident.Priority.MEDIUM, null));
            incidentService.create(tenant1, new CreateIncidentRequest(Incident.Type.ELECTRIC,
                    "Đèn hành lang chập chờn", "Đèn hành lang nhấp nháy vào buổi tối.", Incident.Priority.HIGH, null));
            notificationService.createNotification(tenant1, NotificationType.INCIDENT,
                    "Đã tiếp nhận sự cố", "Chủ nhà đã nhận báo cáo về vòi nước.", waterIncident.id());
            notificationService.createNotification(tenant1, NotificationType.SYSTEM,
                    "Chào mừng đến RoomCare", "Bạn có thể theo dõi thông báo và báo sự cố trên ứng dụng.", null);
            
            Tenant tenant2Obj = new Tenant();
            tenant2Obj.setLandlord(landlord1);
            tenant2Obj.setProperty(property2);
            tenant2Obj.setUser(null); // Chưa có tài khoản
            tenant2Obj.setFullName("Phạm Thị D");
            tenant2Obj.setPhone("0934567890");
            tenant2Obj.setEmail("phamthid@example.com");
            tenant2Obj.setIdentityNumber("079123456789");
            tenant2Obj.setDateOfBirth(LocalDate.of(1998, 8, 20));
            tenant2Obj.setAddress("Long An");
            tenant2Obj.setEmergencyContact("0976543210");
            tenantRepository.save(tenant2Obj);
            
            Tenant tenant3Obj = new Tenant();
            tenant3Obj.setLandlord(landlord2);
            tenant3Obj.setProperty(property3);
            tenant3Obj.setUser(null);
            tenant3Obj.setFullName("Hoàng Văn E");
            tenant3Obj.setPhone("0945678901");
            tenant3Obj.setEmail("hoangvane@example.com");
            tenant3Obj.setIdentityNumber("079234567890");
            tenant3Obj.setDateOfBirth(LocalDate.of(1992, 12, 10));
            tenant3Obj.setAddress("Đồng Nai");
            tenant3Obj.setEmergencyContact("0965432109");
            tenantRepository.save(tenant3Obj);
            
            System.out.println("Development data seeded successfully!");
            System.out.println("Landlord accounts:");
            System.out.println("  - email: landlord1@example.com, password: 123456");
            System.out.println("  - email: landlord2@example.com, password: 123456");
            System.out.println("Tenant accounts:");
            System.out.println("  - email: tenant1@example.com, password: 123456");
    }

    private void seedFinanceAndHealthData() {
        User landlord1 = userRepository.findByEmail("landlord1@example.com").orElse(null);
        if (landlord1 == null) {
            return;
        }

        List<Room> rooms = roomRepository.findByLandlordId(landlord1.getId(), PageRequest.of(0, 50)).getContent();
        Map<String, Room> roomByCode = rooms.stream()
                .collect(Collectors.toMap(Room::getRoomCode, r -> r, (a, b) -> a));

        List<Tenant> tenants = tenantRepository.findByLandlordId(landlord1.getId(), PageRequest.of(0, 50)).getContent();
        Tenant tenantC = tenants.stream().filter(t -> t.getFullName().contains("C")).findFirst().orElse(null);
        Tenant tenantD = tenants.stream().filter(t -> t.getFullName().contains("D")).findFirst().orElse(null);

        YearMonth currentMonth = YearMonth.now();
        String mPrev2 = currentMonth.minusMonths(2).toString();
        String mPrev1 = currentMonth.minusMonths(1).toString();
        String mCurr = currentMonth.toString();

        // 1. Phòng 102 (OCCUPIED - Tốt)
        Room r102 = roomByCode.get("102");
        if (r102 != null) {
            UtilityRecord u102M2 = saveUtility(landlord1, r102, mPrev2, 1000, 1110, 65, 72, 100000, 50000);
            UtilityRecord u102M1 = saveUtility(landlord1, r102, mPrev1, 1110, 1225, 72, 80, 100000, 50000);
            UtilityRecord u102Curr = saveUtility(landlord1, r102, mCurr, 1225, 1345, 80, 88, 100000, 50000);

            Invoice inv102M2 = saveInvoice(landlord1, r102, tenantC, u102M2, mPrev2,
                    LocalDate.now().minusMonths(2).withDayOfMonth(5),
                    new BigDecimal("3640000"), InvoiceStatus.PAID, "Thanh toán đủ kỳ trước");
            savePayment(landlord1, inv102M2, new BigDecimal("3640000"), PaymentMethod.BANK_TRANSFER,
                    "FT" + mPrev2.replace("-", "") + "102", "Chủ nhà xác nhận đã nhận đủ", LocalDateTime.now().minusMonths(2));

            Invoice inv102M1 = saveInvoice(landlord1, r102, tenantC, u102M1, mPrev1,
                    LocalDate.now().minusMonths(1).withDayOfMonth(5),
                    new BigDecimal("3672500"), InvoiceStatus.PAID, "Thanh toán đúng hạn");
            savePayment(landlord1, inv102M1, new BigDecimal("3672500"), PaymentMethod.CASH,
                    "PT-" + mPrev1.replace("-", "") + "-102", "Thu tiền mặt tại phòng", LocalDateTime.now().minusMonths(1));

            Invoice inv102Curr = saveInvoice(landlord1, r102, tenantC, u102Curr, mCurr,
                    LocalDate.now().plusDays(10),
                    new BigDecimal("2000000"), InvoiceStatus.PARTIALLY_PAID, "Đã cọc trước 2 triệu tiền phòng");
            savePayment(landlord1, inv102Curr, new BigDecimal("2000000"), PaymentMethod.BANK_TRANSFER,
                    "FT" + mCurr.replace("-", "") + "102A", "Chuyển khoản đợt 1", LocalDateTime.now().minusDays(1));
        }

        // 2. Phòng 201 (EXPIRING_SOON - Cần theo dõi)
        Room r201 = roomByCode.get("201");
        if (r201 != null) {
            UtilityRecord u201M1 = saveUtility(landlord1, r201, mPrev1, 500, 610, 30, 36, 100000, 50000);
            UtilityRecord u201Curr = saveUtility(landlord1, r201, mCurr, 610, 730, 36, 43, 100000, 50000);

            Invoice inv201M1 = saveInvoice(landlord1, r201, tenantC, u201M1, mPrev1,
                    LocalDate.now().minusMonths(1).withDayOfMonth(5),
                    new BigDecimal("3325000"), InvoiceStatus.PAID, "Hoàn tất kỳ trước");
            savePayment(landlord1, inv201M1, new BigDecimal("3325000"), PaymentMethod.BANK_TRANSFER,
                    "FT" + mPrev1.replace("-", "") + "201", "Chuyển khoản đủ", LocalDateTime.now().minusMonths(1));

            saveInvoice(landlord1, r201, tenantC, u201Curr, mCurr,
                    LocalDate.now().plusDays(8),
                    BigDecimal.ZERO, InvoiceStatus.UNPAID, "Hợp đồng sắp hết hạn, chờ thanh toán kỳ mới");
        }

        // 3. Phòng 2B (NEEDS_ATTENTION - Rủi ro)
        Room r2B = roomByCode.get("2B");
        if (r2B != null) {
            UtilityRecord u2BM2 = saveUtility(landlord1, r2B, mPrev2, 300, 390, 20, 26, 100000, 50000);
            UtilityRecord u2BM1 = saveUtility(landlord1, r2B, mPrev1, 390, 560, 26, 36, 100000, 50000);

            saveInvoice(landlord1, r2B, tenantD, u2BM2, mPrev2,
                    LocalDate.now().minusDays(40),
                    BigDecimal.ZERO, InvoiceStatus.OVERDUE, "Quá hạn chưa thanh toán");

            Invoice inv2BM1 = saveInvoice(landlord1, r2B, tenantD, u2BM1, mPrev1,
                    LocalDate.now().minusDays(12),
                    new BigDecimal("1000000"), InvoiceStatus.OVERDUE, "Mới đóng 1 triệu, điện tăng cao");
            savePayment(landlord1, inv2BM1, new BigDecimal("1000000"), PaymentMethod.CASH,
                    "PT-" + mPrev1.replace("-", "") + "-2B", "Khách hẹn cuối tuần đóng phần còn lại", LocalDateTime.now().minusDays(10));
        }

        System.out.println("Phase 4 (Member 2) finance & health score sample data seeded!");
    }

    private UtilityRecord saveUtility(
            User landlord, Room room, String billingMonth,
            double prevElec, double currElec,
            double prevWater, double currWater,
            double internet, double service) {
        if (utilityRecordRepository.existsByRoomIdAndBillingMonth(room.getId(), billingMonth)) {
            return utilityRecordRepository.findByRoomIdAndBillingMonth(room.getId(), billingMonth).orElse(null);
        }
        UtilityRecord u = new UtilityRecord();
        u.setLandlord(landlord);
        u.setProperty(room.getProperty());
        u.setRoom(room);
        u.setBillingMonth(billingMonth);
        u.setPrevElectricIndex(BigDecimal.valueOf(prevElec));
        u.setCurrElectricIndex(BigDecimal.valueOf(currElec));
        u.setElectricUsage(BigDecimal.valueOf(currElec - prevElec));
        u.setElectricUnitPrice(new BigDecimal("3500"));
        u.setElectricityAmount(u.getElectricUsage().multiply(u.getElectricUnitPrice()));
        u.setPrevWaterIndex(BigDecimal.valueOf(prevWater));
        u.setCurrWaterIndex(BigDecimal.valueOf(currWater));
        u.setWaterUsage(BigDecimal.valueOf(currWater - prevWater));
        u.setWaterUnitPrice(new BigDecimal("15000"));
        u.setWaterAmount(u.getWaterUsage().multiply(u.getWaterUnitPrice()));
        u.setInternetFee(BigDecimal.valueOf(internet));
        u.setServiceFee(BigDecimal.valueOf(service));
        u.setTotalUtilityAmount(u.getElectricityAmount().add(u.getWaterAmount())
                .add(u.getInternetFee()).add(u.getServiceFee()));
        u.setIsAdjustment(false);
        u.setHistoryLog("Khởi tạo dữ liệu mẫu kỳ " + billingMonth);
        return utilityRecordRepository.save(u);
    }

    private Invoice saveInvoice(
            User landlord, Room room, Tenant tenant, UtilityRecord utility,
            String billingMonth, LocalDate dueDate,
            BigDecimal paidAmount, InvoiceStatus status, String notes) {
        Invoice inv = new Invoice();
        inv.setInvoiceCode("INV-" + billingMonth.replace("-", "") + "-" + room.getRoomCode().toUpperCase() + "-R" + room.getId());
        inv.setLandlord(landlord);
        inv.setProperty(room.getProperty());
        inv.setRoom(room);
        inv.setTenant(tenant);
        inv.setUtilityRecordId(utility != null ? utility.getId() : null);
        inv.setBillingMonth(billingMonth);
        inv.setRoomRent(room.getRentPrice());
        inv.setElectricityAmount(utility != null ? utility.getElectricityAmount() : BigDecimal.ZERO);
        inv.setWaterAmount(utility != null ? utility.getWaterAmount() : BigDecimal.ZERO);
        inv.setInternetAmount(utility != null ? utility.getInternetFee() : BigDecimal.ZERO);
        inv.setServiceAmount(utility != null ? utility.getServiceFee() : BigDecimal.ZERO);
        inv.setOtherFees(BigDecimal.ZERO);
        inv.setPreviousDebt(BigDecimal.ZERO);
        inv.setDiscountAmount(BigDecimal.ZERO);

        BigDecimal total = inv.getRoomRent()
                .add(inv.getElectricityAmount())
                .add(inv.getWaterAmount())
                .add(inv.getInternetAmount())
                .add(inv.getServiceAmount());
        BigDecimal safePaid = paidAmount.min(total);
        inv.setTotalAmount(total);
        inv.setPaidAmount(safePaid);
        inv.setRemainingAmount(total.subtract(safePaid));
        inv.setStatus(status);
        inv.setDueDate(dueDate);
        inv.setNotes(notes);
        return invoiceRepository.save(inv);
    }

    private void savePayment(
            User landlord, Invoice invoice, BigDecimal amount,
            PaymentMethod method, String txCode, String note, LocalDateTime paidAt) {
        Payment p = new Payment();
        p.setLandlord(landlord);
        p.setInvoice(invoice);
        p.setAmount(amount);
        p.setPaymentMethod(method);
        p.setTransactionCode(txCode);
        p.setReceiptNote(note);
        p.setPaidAt(paidAt);
        paymentRepository.save(p);
    }
}
