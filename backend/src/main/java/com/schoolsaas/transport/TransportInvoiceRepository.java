package com.schoolsaas.transport;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransportInvoiceRepository extends JpaRepository<TransportInvoice, Long> {

    List<TransportInvoice> findAllByStudentIdOrderByIssuedAtDesc(Long studentId);
}
