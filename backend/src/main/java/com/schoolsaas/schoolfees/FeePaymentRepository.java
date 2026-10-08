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

    /**
     * Le total encaissé sur la période, agrégé par la base.
     *
     * <p>Indispensable dès que le journal est paginé : l'écran ne reçoit qu'une page et ne
     * peut donc plus faire la somme lui-même. Renvoie null sur une période sans règlement,
     * ce que l'appelant ramène à zéro.
     */
    @org.springframework.data.jpa.repository.Query(
            "select sum(p.amountCents) from FeePayment p where p.paidAt >= :from and p.paidAt < :to")
    Long sumAmountBetween(
            @org.springframework.data.repository.query.Param("from") Instant from,
            @org.springframework.data.repository.query.Param("to") Instant to);
}
