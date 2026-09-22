package com.schoolsaas.schoolyear.dto;

import java.util.List;

/** Compte rendu d'une rentrée : ce qui a été réinscrit, et ce qui ne l'a pas été. */
public record PromotionResult(
        Long targetYearId,
        String targetYearLabel,
        int promotedCount,
        int repeatingCount,
        int leftCount,
        List<String> skipped) {
}
