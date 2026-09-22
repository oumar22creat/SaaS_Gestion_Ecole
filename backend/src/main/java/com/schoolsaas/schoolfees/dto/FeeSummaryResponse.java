package com.schoolsaas.schoolfees.dto;

import com.schoolsaas.schoolfees.FeePaymentMethod;
import java.util.List;

/**
 * Indicateurs de recouvrement, pour le tableau de bord comme pour l'écran Frais scolaires
 * (cahier-des-charges.md §19.4).
 *
 * <p>{@code collectedCents} est ce qui a été encaissé sur les factures du périmètre, toutes
 * dates confondues ; {@code overdueCents} est la part du reste à recouvrer dont l'échéance
 * est dépassée. Les deux répondent à des questions différentes : « combien est rentré » et
 * « combien aurait dû être rentré ».
 *
 * <p>Les factures annulées sont exclues de tous les totaux : elles ne sont ni dues ni
 * recouvrables, les compter fausserait le taux de recouvrement.
 */
public record FeeSummaryResponse(
        long invoicedCents,
        long collectedCents,
        long outstandingCents,
        long overdueCents,
        Double collectionRate,
        long invoiceCount,
        long settledInvoiceCount,
        long overdueInvoiceCount,
        long lateStudentCount,
        String currency,
        List<CollectionByMethod> collectionByMethod) {

    /** Ventilation des encaissements par moyen de paiement, pour le rapprochement de caisse. */
    public record CollectionByMethod(FeePaymentMethod method, long amountCents, long paymentCount) {
    }
}
