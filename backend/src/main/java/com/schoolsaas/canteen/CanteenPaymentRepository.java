package com.schoolsaas.canteen;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CanteenPaymentRepository extends JpaRepository<CanteenPayment, Long> {

    List<CanteenPayment> findAllByInvoiceId(Long invoiceId);
}
