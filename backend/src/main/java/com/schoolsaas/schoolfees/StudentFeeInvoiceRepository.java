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

    /**
     * Les factures d'un échéancier hors certains statuts. Écarter les soldées et les
     * annulées en base plutôt qu'en mémoire : en fin d'année elles sont la majorité, et les
     * charger pour les jeter ensuite fait travailler la base pour rien.
     */
    List<StudentFeeInvoice> findAllByFeeScheduleIdInAndStatusNotIn(
            List<Long> feeScheduleIds, List<FeeInvoiceStatus> statuses);
}
