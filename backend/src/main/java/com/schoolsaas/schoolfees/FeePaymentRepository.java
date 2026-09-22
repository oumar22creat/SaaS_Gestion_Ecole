package com.schoolsaas.schoolfees;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeePaymentRepository extends JpaRepository<FeePayment, Long> {

    List<FeePayment> findAllByInvoiceId(Long invoiceId);

    List<FeePayment> findAllByInvoiceIdIn(List<Long> invoiceIds);

    List<FeePayment> findAllByPaidAtBetweenOrderByPaidAtDesc(Instant from, Instant to);
}
