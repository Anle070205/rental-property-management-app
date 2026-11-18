package com.roomcare.backend;

import com.roomcare.backend.dto.debt.DebtSummaryResponse;
import com.roomcare.backend.dto.health.RoomHealthScoreResponse;
import com.roomcare.backend.dto.invoice.CreateInvoiceRequest;
import com.roomcare.backend.dto.invoice.CreatePaymentRequest;
import com.roomcare.backend.dto.invoice.InvoiceResponse;
import com.roomcare.backend.dto.utility.CreateUtilityRequest;
import com.roomcare.backend.dto.utility.UpdateUtilityRequest;
import com.roomcare.backend.dto.utility.UtilityRecordResponse;
import com.roomcare.backend.entity.*;
import com.roomcare.backend.repository.*;
import com.roomcare.backend.service.DebtService;
import com.roomcare.backend.service.HealthScoreService;
import com.roomcare.backend.service.InvoiceService;
import com.roomcare.backend.service.UtilityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FinanceAndHealthScoreTest {

    @Mock private InvoiceRepository invoiceRepository;
    @Mock private PaymentRepository paymentRepository;
    @Mock private RoomRepository roomRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private UserRepository userRepository;
    @Mock private UtilityRecordRepository utilityRecordRepository;

    @InjectMocks private InvoiceService invoiceService;
    @InjectMocks private UtilityService utilityService;
    @InjectMocks private DebtService debtService;
    @InjectMocks private HealthScoreService healthScoreService;

    private User landlord;
    private Property property;
    private Room roomOccupied;
    private Room roomExpiring;
    private Room roomAttention;
    private Tenant tenant;

    @BeforeEach
    void setUp() {
        landlord = new User();
        landlord.setId(1L);
        landlord.setFullName("Nguyễn Văn A");
        landlord.setRole(Role.LANDLORD);

        property = new Property();
        property.setId(10L);
        property.setLandlord(landlord);
        property.setName("Nhà trọ Hoàng Anh");

        roomOccupied = new Room();
        roomOccupied.setId(100L);
        roomOccupied.setProperty(property);
        roomOccupied.setRoomCode("102");
        roomOccupied.setRentPrice(new BigDecimal("3000000"));
        roomOccupied.setStatus(RoomStatus.OCCUPIED);

        roomExpiring = new Room();
        roomExpiring.setId(101L);
        roomExpiring.setProperty(property);
        roomExpiring.setRoomCode("201");
        roomExpiring.setRentPrice(new BigDecimal("2700000"));
        roomExpiring.setStatus(RoomStatus.EXPIRING_SOON);

        roomAttention = new Room();
        roomAttention.setId(102L);
        roomAttention.setProperty(property);
        roomAttention.setRoomCode("2B");
        roomAttention.setRentPrice(new BigDecimal("2300000"));
        roomAttention.setStatus(RoomStatus.NEEDS_ATTENTION);

        tenant = new Tenant();
        tenant.setId(50L);
        tenant.setLandlord(landlord);
        tenant.setProperty(property);
        tenant.setFullName("Lê Văn C");
        tenant.setPhone("0923456789");
    }

    @Test
    void createInvoice_autoFillsRentAndLinkedUtility() {
        String month = YearMonth.now().toString();
        UtilityRecord util = new UtilityRecord();
        util.setId(77L);
        util.setRoom(roomOccupied);
        util.setBillingMonth(month);
        util.setElectricityAmount(new BigDecimal("350000"));
        util.setWaterAmount(new BigDecimal("90000"));
        util.setInternetFee(new BigDecimal("100000"));
        util.setServiceFee(new BigDecimal("50000"));
        util.setElectricUsage(new BigDecimal("100"));
        util.setWaterUsage(new BigDecimal("6"));

        when(roomRepository.findById(100L)).thenReturn(Optional.of(roomOccupied));
        when(invoiceRepository.existsByRoomIdAndBillingMonth(100L, month)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(landlord));
        when(tenantRepository.findById(50L)).thenReturn(Optional.of(tenant));
        when(utilityRecordRepository.findByRoomIdAndBillingMonth(100L, month)).thenReturn(Optional.of(util));
        when(utilityRecordRepository.findById(77L)).thenReturn(Optional.of(util));
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> {
            Invoice i = inv.getArgument(0);
            i.setId(1L);
            return i;
        });

        CreateInvoiceRequest req = new CreateInvoiceRequest();
        req.setRoomId(100L);
        req.setTenantId(50L);
        req.setBillingMonth(month);
        req.setDueDate(LocalDate.now().plusDays(10));
        req.setOtherFees(new BigDecimal("50000"));

        InvoiceResponse res = invoiceService.createInvoice(1L, req);
        assertEquals(new BigDecimal("3640000"), res.getTotalAmount());
        assertEquals(new BigDecimal("3640000"), res.getRemainingAmount());
        assertEquals(InvoiceStatus.UNPAID, res.getStatus());
        assertEquals(77L, res.getUtilityRecordId());
    }

    @Test
    void createInvoice_rejectsDuplicateRoomAndMonth() {
        String month = YearMonth.now().toString();
        when(roomRepository.findById(100L)).thenReturn(Optional.of(roomOccupied));
        when(invoiceRepository.existsByRoomIdAndBillingMonth(100L, month)).thenReturn(true);

        CreateInvoiceRequest req = new CreateInvoiceRequest();
        req.setRoomId(100L);
        req.setBillingMonth(month);
        req.setDueDate(LocalDate.now().plusDays(5));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> invoiceService.createInvoice(1L, req));
        assertTrue(ex.getMessage().contains("đã có hóa đơn"));
    }

    @Test
    void utility_blocksDecreasingIndexWithoutAdjustmentAndAllowsWithReason() {
        String month = YearMonth.now().toString();
        when(roomRepository.findById(100L)).thenReturn(Optional.of(roomOccupied));
        when(utilityRecordRepository.existsByRoomIdAndBillingMonth(100L, month)).thenReturn(false);
        when(userRepository.findById(1L)).thenReturn(Optional.of(landlord));

        CreateUtilityRequest invalidReq = new CreateUtilityRequest(
                100L, month,
                new BigDecimal("500"), new BigDecimal("450"), new BigDecimal("3500"),
                new BigDecimal("20"), new BigDecimal("25"), new BigDecimal("15000"),
                new BigDecimal("100000"), new BigDecimal("50000"),
                false, null, null
        );

        assertThrows(RuntimeException.class, () -> utilityService.createUtilityRecord(1L, invalidReq));

        when(utilityRecordRepository.save(any(UtilityRecord.class))).thenAnswer(inv -> {
            UtilityRecord r = inv.getArgument(0);
            r.setId(9L);
            return r;
        });
        when(invoiceRepository.findByRoomIdAndBillingMonth(100L, month)).thenReturn(Optional.empty());

        CreateUtilityRequest validAdjReq = new CreateUtilityRequest(
                100L, month,
                new BigDecimal("500"), new BigDecimal("40"), new BigDecimal("3500"),
                new BigDecimal("20"), new BigDecimal("25"), new BigDecimal("15000"),
                new BigDecimal("100000"), new BigDecimal("50000"),
                true, "Thay đồng hồ điện mới từ 0", null
        );

        UtilityRecordResponse res = utilityService.createUtilityRecord(1L, validAdjReq);
        assertEquals(new BigDecimal("40"), res.getElectricUsage());
        assertEquals(new BigDecimal("365000.00"), res.getTotalUtilityAmount());
    }

    @Test
    void utility_blocksUpdateWhenLinkedInvoiceIsPaid() {
        String month = YearMonth.now().toString();
        UtilityRecord existing = new UtilityRecord();
        existing.setId(15L);
        existing.setLandlord(landlord);
        existing.setProperty(property);
        existing.setRoom(roomOccupied);
        existing.setBillingMonth(month);

        Invoice paidInvoice = new Invoice();
        paidInvoice.setId(20L);
        paidInvoice.setStatus(InvoiceStatus.PAID);

        when(utilityRecordRepository.findById(15L)).thenReturn(Optional.of(existing));
        when(invoiceRepository.findByRoomIdAndBillingMonth(100L, month)).thenReturn(Optional.of(paidInvoice));

        UpdateUtilityRequest updateReq = new UpdateUtilityRequest(
                BigDecimal.ZERO, new BigDecimal("100"), new BigDecimal("3500"),
                BigDecimal.ZERO, new BigDecimal("5"), new BigDecimal("15000"),
                BigDecimal.ZERO, BigDecimal.ZERO, false, null, null
        );

        assertThrows(RuntimeException.class, () -> utilityService.updateUtilityRecord(1L, 15L, updateReq));
    }

    @Test
    void recordPayment_handlesPartialAndFullPaymentAndRejectsUnconfirmed() {
        Invoice invoice = new Invoice();
        invoice.setId(30L);
        invoice.setInvoiceCode("INV-202611-102-R100");
        invoice.setLandlord(landlord);
        invoice.setProperty(property);
        invoice.setRoom(roomOccupied);
        invoice.setTenant(tenant);
        invoice.setBillingMonth(YearMonth.now().toString());
        invoice.setTotalAmount(new BigDecimal("3500000"));
        invoice.setPaidAmount(BigDecimal.ZERO);
        invoice.setRemainingAmount(new BigDecimal("3500000"));
        invoice.setStatus(InvoiceStatus.UNPAID);
        invoice.setDueDate(LocalDate.now().plusDays(5));

        CreatePaymentRequest unconfirmed = new CreatePaymentRequest(
                new BigDecimal("1500000"), PaymentMethod.BANK_TRANSFER, "QR-VIEW", null, false
        );
        assertThrows(RuntimeException.class, () -> invoiceService.recordPayment(1L, 30L, unconfirmed));

        when(invoiceRepository.findById(30L)).thenReturn(Optional.of(invoice));
        when(userRepository.findById(1L)).thenReturn(Optional.of(landlord));
        when(paymentRepository.existsByInvoiceIdAndTransactionCode(30L, "FT001")).thenReturn(false);
        when(paymentRepository.existsByInvoiceIdAndAmountAndPaymentMethodAndPaidAtAfter(any(), any(), any(), any()))
                .thenReturn(false);
        when(invoiceRepository.save(any(Invoice.class))).thenAnswer(inv -> inv.getArgument(0));
        when(paymentRepository.findByInvoiceIdOrderByPaidAtDesc(30L)).thenReturn(List.of());

        CreatePaymentRequest partialReq = new CreatePaymentRequest(
                new BigDecimal("1500000"), PaymentMethod.BANK_TRANSFER, "FT001", "Trả đợt 1", true
        );
        InvoiceResponse afterPartial = invoiceService.recordPayment(1L, 30L, partialReq);
        assertEquals(InvoiceStatus.PARTIALLY_PAID, afterPartial.getStatus());
        assertEquals(new BigDecimal("2000000"), afterPartial.getRemainingAmount());

        when(paymentRepository.existsByInvoiceIdAndTransactionCode(30L, "FT001")).thenReturn(true);
        assertThrows(RuntimeException.class, () -> invoiceService.recordPayment(1L, 30L, partialReq));

        CreatePaymentRequest finalReq = new CreatePaymentRequest(
                new BigDecimal("2000000"), PaymentMethod.CASH, "PT002", "Trả phần còn lại", true
        );
        when(paymentRepository.existsByInvoiceIdAndTransactionCode(30L, "PT002")).thenReturn(false);
        InvoiceResponse afterFull = invoiceService.recordPayment(1L, 30L, finalReq);
        assertEquals(InvoiceStatus.PAID, afterFull.getStatus());
        assertEquals(BigDecimal.ZERO, afterFull.getRemainingAmount());
    }

    @Test
    void debtService_computesOverdueAndPartialBalancesAccurately() {
        Invoice overdueInv = new Invoice();
        overdueInv.setId(40L);
        overdueInv.setInvoiceCode("INV-202610-2B");
        overdueInv.setLandlord(landlord);
        overdueInv.setProperty(property);
        overdueInv.setRoom(roomAttention);
        overdueInv.setTenant(tenant);
        overdueInv.setBillingMonth(YearMonth.now().minusMonths(1).toString());
        overdueInv.setTotalAmount(new BigDecimal("2500000"));
        overdueInv.setPaidAmount(new BigDecimal("1000000"));
        overdueInv.setRemainingAmount(new BigDecimal("1500000"));
        overdueInv.setStatus(InvoiceStatus.OVERDUE);
        overdueInv.setDueDate(LocalDate.now().minusDays(10));

        when(invoiceRepository.findAllByLandlordWithDetails(1L)).thenReturn(List.of(overdueInv));
        when(paymentRepository.findByInvoiceIdOrderByPaidAtDesc(40L)).thenReturn(List.of());

        DebtSummaryResponse summary = debtService.getDebts(1L, null, null, null, null, null);
        assertEquals(new BigDecimal("1500000"), summary.getTotalDebtAmount());
        assertEquals(1, summary.getTotalOverdueInvoices());
        assertEquals("OVERDUE_8_30", summary.getItems().get(0).getOverdueLevel());
    }

    @Test
    void healthScore_evaluatesGoodWarningRiskAndMissingData() {
        String currMonth = YearMonth.now().toString();
        String prevMonth = YearMonth.now().minusMonths(1).toString();

        // GOOD scenario
        Invoice paidInv = new Invoice();
        paidInv.setStatus(InvoiceStatus.PAID);
        paidInv.setRemainingAmount(BigDecimal.ZERO);
        paidInv.setPaidAmount(new BigDecimal("3500000"));
        paidInv.setTotalAmount(new BigDecimal("3500000"));
        paidInv.setDueDate(LocalDate.now().plusDays(5));

        UtilityRecord utilGood = new UtilityRecord();
        utilGood.setBillingMonth(currMonth);
        utilGood.setElectricUsage(new BigDecimal("110"));
        utilGood.setWaterUsage(new BigDecimal("7"));
        utilGood.setIsAdjustment(false);

        when(invoiceRepository.findByRoomIdOrderByBillingMonthDesc(100L)).thenReturn(List.of(paidInv));
        when(utilityRecordRepository.findByRoomIdOrderByBillingMonthDesc(100L)).thenReturn(List.of(utilGood));

        RoomHealthScoreResponse goodScore = healthScoreService.calculateRoomHealthScore(roomOccupied);
        assertEquals(100, goodScore.getTotalScore());
        assertEquals("GOOD", goodScore.getRatingLevel());

        // WARNING scenario (Expiring soon + 1 overdue invoice 5 days + missing utility 16/20 -> 28+15+16+15 = 74)
        Invoice overdueCurr = new Invoice();
        overdueCurr.setStatus(InvoiceStatus.OVERDUE);
        overdueCurr.setRemainingAmount(new BigDecimal("3000000"));
        overdueCurr.setPaidAmount(BigDecimal.ZERO);
        overdueCurr.setTotalAmount(new BigDecimal("3000000"));
        overdueCurr.setDueDate(LocalDate.now().minusDays(5));

        when(invoiceRepository.findByRoomIdOrderByBillingMonthDesc(101L)).thenReturn(List.of(overdueCurr));
        when(utilityRecordRepository.findByRoomIdOrderByBillingMonthDesc(101L)).thenReturn(List.of());

        RoomHealthScoreResponse warningScore = healthScoreService.calculateRoomHealthScore(roomExpiring);
        assertEquals(74, warningScore.getTotalScore());
        assertEquals("WARNING", warningScore.getRatingLevel());

        // Missing-data fallback scenario (VACANT room with no invoices & no utilities -> 35+12+16+15 = 78)
        Room roomVacant = new Room();
        roomVacant.setId(103L);
        roomVacant.setProperty(property);
        roomVacant.setRoomCode("101");
        roomVacant.setStatus(RoomStatus.VACANT);
        when(invoiceRepository.findByRoomIdOrderByBillingMonthDesc(103L)).thenReturn(List.of());
        when(utilityRecordRepository.findByRoomIdOrderByBillingMonthDesc(103L)).thenReturn(List.of());
        RoomHealthScoreResponse missingDataScore = healthScoreService.calculateRoomHealthScore(roomVacant);
        assertEquals(78, missingDataScore.getTotalScore());
        assertTrue(missingDataScore.getFactors().get(0).getDetail().contains("điểm cơ sở"));

        // RISK scenario (NEEDS_ATTENTION + 2 overdue > 15 days + spike > 50%)
        Invoice od1 = new Invoice();
        od1.setStatus(InvoiceStatus.OVERDUE);
        od1.setRemainingAmount(new BigDecimal("2500000"));
        od1.setPaidAmount(BigDecimal.ZERO);
        od1.setDueDate(LocalDate.now().minusDays(25));

        Invoice od2 = new Invoice();
        od2.setStatus(InvoiceStatus.OVERDUE);
        od2.setRemainingAmount(new BigDecimal("1500000"));
        od2.setPaidAmount(new BigDecimal("1000000"));
        od2.setDueDate(LocalDate.now().minusDays(10));

        UtilityRecord uNew = new UtilityRecord();
        uNew.setBillingMonth(prevMonth);
        uNew.setElectricUsage(new BigDecimal("200"));
        uNew.setWaterUsage(new BigDecimal("12"));
        uNew.setIsAdjustment(false);

        UtilityRecord uOld = new UtilityRecord();
        uOld.setBillingMonth(YearMonth.now().minusMonths(2).toString());
        uOld.setElectricUsage(new BigDecimal("100"));
        uOld.setWaterUsage(new BigDecimal("6"));
        uOld.setIsAdjustment(false);

        when(invoiceRepository.findByRoomIdOrderByBillingMonthDesc(102L)).thenReturn(List.of(od2, od1));
        when(utilityRecordRepository.findByRoomIdOrderByBillingMonthDesc(102L)).thenReturn(List.of(uNew, uOld));

        RoomHealthScoreResponse riskScore = healthScoreService.calculateRoomHealthScore(roomAttention);
        assertTrue(riskScore.getTotalScore() < 50);
        assertEquals("RISK", riskScore.getRatingLevel());
        assertFalse(riskScore.getReasonsForDeduction().isEmpty());
        assertFalse(riskScore.getRecommendations().isEmpty());
    }
}
