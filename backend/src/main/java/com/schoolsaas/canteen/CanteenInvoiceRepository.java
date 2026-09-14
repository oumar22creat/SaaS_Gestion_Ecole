package com.schoolsaas.canteen;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CanteenInvoiceRepository extends JpaRepository<CanteenInvoice, Long> {

    List<CanteenInvoice> findAllByStudentIdOrderByIssuedAtDesc(Long studentId);

    List<CanteenInvoice> findAllByStatusNot(CanteenInvoiceStatus status);
}
