package com.schoolsaas.platformadmin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Positive;

/** Prolongation d'essai : un geste commercial, borné pour rester un geste. */
public record TrialExtensionRequest(@Positive @Max(180) int days, String reason) {
}
