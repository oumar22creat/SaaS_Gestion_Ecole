package com.schoolsaas.schoolyear.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record SchoolYearRequest(
        @NotBlank @Size(max = 50) String label,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate) {
}
