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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;

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
    private final PasswordEncoder passwordEncoder;
    private final IncidentService incidentService;
    private final NotificationService notificationService;
    
    @Bean
    public CommandLineRunner seedData() {
        return args -> {
            // Kiểm tra nếu đã có data thì không seed nữa
            if (userRepository.count() > 0) {
                System.out.println("Database already has data. Skipping seed.");
                return;
            }
            
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
        };
    }
}
