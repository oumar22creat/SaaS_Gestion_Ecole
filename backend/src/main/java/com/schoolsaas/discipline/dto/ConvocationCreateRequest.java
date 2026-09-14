package com.schoolsaas.discipline.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;

public record ConvocationCreateRequest(
        @NotNull Long studentId, boolean convokeParent, @NotBlank String reason, @NotNull Instant scheduledAt) {
}
