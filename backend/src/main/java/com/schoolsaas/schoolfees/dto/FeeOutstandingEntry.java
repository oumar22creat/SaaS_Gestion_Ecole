package com.schoolsaas.schoolfees.dto;

import com.schoolsaas.schoolfees.FeeInvoiceStatus;
import java.time.LocalDate;

/**
 * Une facture non soldée, enrichie de tout ce qu'il faut pour agir : qui doit, combien, et
 * depuis quand. Le comptable travaille sur des noms d'élèves, pas sur des identifiants de
 * factures.
 *
 * <p>{@code overdue} et {@code daysLate} sont calculés à la lecture en comparant l'échéance à
 * la date du jour, jamais stockés : un statut « en retard » figé en base deviendrait faux
 * dès le lendemain sans traitement planifié pour le rafraîchir.
 */
public record FeeOutstandingEntry(
        Long invoiceId,
        Long studentId,
        String studentName,
        String studentNumber,
        Long schoolClassId,
        String className,
        String scheduleLabel,
        LocalDate dueDate,
        long amountDueCents,
        long amountPaidCents,
        long amountRemainingCents,
        FeeInvoiceStatus status,
        boolean overdue,
        long daysLate) {
}
