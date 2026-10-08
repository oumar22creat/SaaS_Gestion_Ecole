package com.schoolsaas.grade.dto;

import com.schoolsaas.grade.ExamType;
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
        @NotNull LocalDate examDate,

        /**
         * Facultatif : absent, l'évaluation est un devoir. C'est le cas courant, et une école
         * qui ne distingue pas les deux ne doit pas avoir à renseigner un champ de plus.
         */
        ExamType examType) {

    public ExamType examTypeOrDefault() {
        return examType == null ? ExamType.DEVOIR : examType;
    }
}
