package com.schoolsaas.schoolfees.dto;

import com.schoolsaas.schoolfees.FeePaymentMethod;
import java.time.Instant;

/**
 * Une ligne du journal des encaissements : ce que le comptable relit en fin de journée pour
 * rapprocher sa caisse. {@code recordedByUserId} vide signale un encaissement automatique
 * (mobile money), que personne n'a saisi — voir {@link com.schoolsaas.schoolfees.FeePaymentMethod}.
 */
public record FeePaymentJournalEntry(
        Long paymentId,
        Long invoiceId,
        Long studentId,
        String studentName,
        String className,
        String scheduleLabel,
        long amountCents,
        FeePaymentMethod method,
        String reference,
        Long recordedByUserId,
        Instant paidAt) {
}
