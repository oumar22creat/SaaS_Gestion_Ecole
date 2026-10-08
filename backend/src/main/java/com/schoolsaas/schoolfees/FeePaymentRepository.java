package com.schoolsaas.schoolfees;

import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeePaymentRepository extends JpaRepository<FeePayment, Long> {

    List<FeePayment> findAllByInvoiceId(Long invoiceId);

    List<FeePayment> findAllByInvoiceIdIn(List<Long> invoiceIds);

    List<FeePayment> findAllByPaidAtBetweenOrderByPaidAtDesc(Instant from, Instant to);

    /**
     * Le journal des encaissements, page par page. Une école de mille élèves enregistre
     * plusieurs milliers de règlements par trimestre : les charger tous pour en afficher
     * vingt fait travailler la base et le serveur pour rien.
     */
    org.springframework.data.domain.Page<FeePayment> findAllByPaidAtBetweenOrderByPaidAtDesc(
            Instant from, Instant to, org.springframework.data.domain.Pageable pageable);
}
