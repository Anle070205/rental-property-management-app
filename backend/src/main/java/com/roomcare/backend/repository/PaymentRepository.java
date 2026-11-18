package com.roomcare.backend.repository;

import com.roomcare.backend.entity.Payment;
import com.roomcare.backend.entity.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByInvoiceIdOrderByPaidAtDesc(Long invoiceId);

    boolean existsByInvoiceIdAndTransactionCode(Long invoiceId, String transactionCode);

    boolean existsByInvoiceIdAndAmountAndPaymentMethodAndPaidAtAfter(
        Long invoiceId,
        BigDecimal amount,
        PaymentMethod paymentMethod,
        LocalDateTime after
    );

    List<Payment> findByLandlordIdOrderByPaidAtDesc(Long landlordId);
}
