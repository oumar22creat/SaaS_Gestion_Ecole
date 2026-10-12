package com.schoolsaas.schoolfees.dto;

import java.util.List;

/**
 * Ce qu'a produit la correction d'une échéance.
 *
 * <p>Le détail compte autant que le succès : corriger le montant d'une échéance répercute le
 * nouveau tarif sur les factures déjà émises, et certaines ne peuvent pas suivre — celles
 * des familles qui ont déjà versé davantage. Les taire laisserait l'établissement croire que
 * toute la classe est au nouveau tarif, alors que trois élèves sont restés à l'ancien.
 *
 * @param updatedInvoices nombre de factures passées au nouveau montant
 * @param skipped         factures laissées à l'ancien montant, avec le nom de l'élève et la
 *                        raison, pour que le comptable sache exactement qui traiter à la main
 */
public record FeeScheduleUpdateResult(
        FeeScheduleResponse schedule, int updatedInvoices, List<String> skipped) {
}
