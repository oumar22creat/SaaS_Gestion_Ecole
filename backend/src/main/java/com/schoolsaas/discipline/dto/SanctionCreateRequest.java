package com.schoolsaas.discipline.dto;

import com.schoolsaas.discipline.SanctionType;
import jakarta.validation.constraints.NotNull;

public record SanctionCreateRequest(
        @NotNull Long studentId, @NotNull SanctionType type, Integer durationDays, String description) {
}
