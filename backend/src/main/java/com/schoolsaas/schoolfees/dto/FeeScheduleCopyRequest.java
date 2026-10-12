package com.schoolsaas.schoolfees.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Recopie l'échéancier d'une classe vers d'autres.
 *
 * <p>Les montants sont repris tels quels, sans ajustement par niveau. Un tarif progressant
 * avec la classe est fréquent, mais la progression n'est pas la même d'une école à l'autre —
 * un pourcentage, un palier, rien du tout. Plutôt que d'imposer une règle qui conviendrait
 * à peu de monde, la recopie pose la base et l'échéance se corrige ensuite classe par
 * classe, ce qui est désormais possible.
 */
public record FeeScheduleCopyRequest(
        @NotNull Long sourceClassId,
        @NotEmpty List<Long> targetClassIds) {
}
