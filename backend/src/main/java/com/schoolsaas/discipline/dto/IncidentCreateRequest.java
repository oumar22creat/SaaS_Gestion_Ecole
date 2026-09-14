package com.schoolsaas.discipline.dto;

import com.schoolsaas.discipline.IncidentSeverity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

public record IncidentCreateRequest(
        @NotNull Long schoolClassId,
        @NotNull LocalDate occurredAt,
        @NotNull IncidentSeverity severity,
        @NotBlank String description,
        @NotEmpty List<Long> studentIds) {
}
