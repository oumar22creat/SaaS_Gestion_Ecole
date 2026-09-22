package com.schoolsaas.schoolfees;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StudentFeeInvoiceRepository extends JpaRepository<StudentFeeInvoice, Long> {

    List<StudentFeeInvoice> findAllByFeeScheduleId(Long feeScheduleId);

    List<StudentFeeInvoice> findAllByStudentIdOrderByIssuedAtDesc(Long studentId);

    Optional<StudentFeeInvoice> findByFeeScheduleIdAndStudentId(Long feeScheduleId, Long studentId);

    List<StudentFeeInvoice> findAllByStatusNot(FeeInvoiceStatus status);

    List<StudentFeeInvoice> findAllByFeeScheduleIdIn(List<Long> feeScheduleIds);
}
