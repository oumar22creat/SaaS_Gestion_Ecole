package com.schoolsaas.schoolfees.dto;

import java.util.List;

/**
 * Ce qu'a produit une recopie d'échéancier.
 *
 * @param created  échéances créées, prêtes à être corrigées puis facturées
 * @param skipped  ce qui n'a pas été recopié, et pourquoi. Presque toujours : la classe
 *                 cible avait déjà une échéance du même intitulé. En créer une seconde
 *                 ferait deux factures « Scolarité 1er trimestre » par élève, donc une dette
 *                 doublée que personne ne comprendrait avant la relance.
 */
public record FeeScheduleCopyResult(List<FeeScheduleResponse> created, List<String> skipped) {
}
