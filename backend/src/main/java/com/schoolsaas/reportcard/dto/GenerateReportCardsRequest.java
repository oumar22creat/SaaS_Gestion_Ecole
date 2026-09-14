package com.schoolsaas.reportcard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record GenerateReportCardsRequest(
        @NotNull Long schoolClassId, @NotBlank String periodLabel, @NotNull LocalDate periodFrom, @NotNull LocalDate periodTo) {
}
