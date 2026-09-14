package com.schoolsaas.transport;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransportPaymentRepository extends JpaRepository<TransportPayment, Long> {

    List<TransportPayment> findAllByInvoiceId(Long invoiceId);
}
