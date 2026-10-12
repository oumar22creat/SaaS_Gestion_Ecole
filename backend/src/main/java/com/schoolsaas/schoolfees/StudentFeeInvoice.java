package com.schoolsaas.schoolfees;

import com.schoolsaas.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;

/** Facture d'un élève pour une ligne de grille tarifaire (cahier-des-charges.md §19.4). */
@Entity
@Table(name = "student_fee_invoices")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class StudentFeeInvoice extends TenantScopedEntity {

    @Column(name = "fee_schedule_id", nullable = false, updatable = false)
    private Long feeScheduleId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private Long studentId;

    /**
     * Montant NET réellement dû, remise déduite. Tout le reste du produit lit cette colonne :
     * reste à payer, impayés, taux de recouvrement, reçus, portail famille.
     */
    @Column(name = "amount_due_cents", nullable = false)
    private long amountDueCents;

    /**
     * Réduction accordée sur le montant de l'échéancier : bourse, tarif fratrie, exonération.
     * Conservée à part du net pour rester lisible après coup, même si l'échéancier de la
     * classe change entre-temps.
     */
    @Column(name = "discount_cents", nullable = false)
    private long discountCents;

    @Column(name = "discount_reason")
    private String discountReason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FeeInvoiceStatus status;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt;

    protected StudentFeeInvoice() {
    }

    public StudentFeeInvoice(Long feeScheduleId, Long studentId, long amountDueCents) {
        this.feeScheduleId = feeScheduleId;
        this.studentId = studentId;
        this.amountDueCents = amountDueCents;
        this.status = FeeInvoiceStatus.PENDING;
        this.issuedAt = Instant.now();
    }

    public Long getFeeScheduleId() {
        return feeScheduleId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public long getAmountDueCents() {
        return amountDueCents;
    }

    public FeeInvoiceStatus getStatus() {
        return status;
    }

    public void setStatus(FeeInvoiceStatus status) {
        this.status = status;
    }

    public Instant getIssuedAt() {
        return issuedAt;
    }

    public long getDiscountCents() {
        return discountCents;
    }

    public String getDiscountReason() {
        return discountReason;
    }

    /** Montant de l'échéancier avant remise : c'est le net et la remise réunis. */
    public long getGrossAmountCents() {
        return amountDueCents + discountCents;
    }

    /**
     * Applique une remise sur le montant d'origine, et non sur le net courant : rejouer
     * l'opération corrige la remise au lieu de l'empiler.
     */
    public void applyDiscount(long discountCents, String reason) {
        long brut = getGrossAmountCents();
        this.discountCents = discountCents;
        this.discountReason = discountCents == 0 ? null : reason;
        this.amountDueCents = brut - discountCents;
    }

    /**
     * Rebase la facture sur un nouveau tarif de classe, remise conservée. Utilisé quand
     * l'établissement corrige le montant d'une échéance après avoir facturé : la réduction
     * accordée à cette famille n'a aucune raison de disparaître avec la correction.
     */
    public void rebase(long grossAmountCents) {
        this.amountDueCents = grossAmountCents - this.discountCents;
    }
}
