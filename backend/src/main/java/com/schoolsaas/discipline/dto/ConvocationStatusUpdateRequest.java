package com.schoolsaas.discipline.dto;

import com.schoolsaas.discipline.ConvocationStatus;
import jakarta.validation.constraints.NotNull;

public record ConvocationStatusUpdateRequest(@NotNull ConvocationStatus status) {
}
