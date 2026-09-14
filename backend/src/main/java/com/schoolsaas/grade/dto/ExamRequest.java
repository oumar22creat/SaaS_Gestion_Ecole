package com.schoolsaas.grade.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record ExamRequest(
        @NotNull Long schoolClassId,
        @NotNull Long subjectId,
        @NotBlank String label,
        @Positive double maxScore,
        @Min(1) int coefficient,
        @NotNull LocalDate examDate) {
}
