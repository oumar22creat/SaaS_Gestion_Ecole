package com.schoolsaas.discipline.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ObservationCreateRequest(@NotNull Long studentId, boolean positive, @NotBlank String description) {
}
