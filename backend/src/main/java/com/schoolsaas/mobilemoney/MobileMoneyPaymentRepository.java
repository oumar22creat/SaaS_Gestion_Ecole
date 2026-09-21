package com.schoolsaas.mobilemoney;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MobileMoneyPaymentRepository extends JpaRepository<MobileMoneyPayment, Long> {

    Optional<MobileMoneyPayment> findByReference(String reference);

    List<MobileMoneyPayment> findAllByInvoiceId(Long invoiceId);
}
