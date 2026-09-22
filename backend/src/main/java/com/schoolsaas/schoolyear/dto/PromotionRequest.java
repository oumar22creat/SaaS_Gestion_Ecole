package com.schoolsaas.schoolyear.dto;

import com.schoolsaas.schoolyear.EnrollmentStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * Passage d'une année à la suivante. Le client envoie une décision par élève : c'est
 * volontairement explicite plutôt que déduit d'une moyenne, car le conseil de classe tranche
 * des cas que le logiciel ne connaît pas (dossier, absence longue, décision des parents).
 */
public record PromotionRequest(
        @NotNull Long sourceYearId,
        @NotEmpty @Valid List<Decision> decisions) {

    /**
     * {@code targetClassId} n'est requis que pour PROMOTED. Un redoublant reprend la classe
     * qu'il avait ; un élève qui part n'est pas réinscrit du tout.
     */
    public record Decision(
            @NotNull Long studentId,
            @NotNull EnrollmentStatus outcome,
            Long targetClassId) {
    }
}
