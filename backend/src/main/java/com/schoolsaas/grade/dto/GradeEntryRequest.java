package com.schoolsaas.grade.dto;

import jakarta.validation.constraints.NotNull;

public record GradeEntryRequest(@NotNull Long studentId, Double score, boolean absent, String comment) {
}
