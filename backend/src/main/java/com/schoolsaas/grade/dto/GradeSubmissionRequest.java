package com.schoolsaas.grade.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/** Saisie individuelle ou en masse des notes d'une évaluation (cahier-des-charges.md §11). */
public record GradeSubmissionRequest(@NotEmpty @Valid List<GradeEntryRequest> entries) {
}
