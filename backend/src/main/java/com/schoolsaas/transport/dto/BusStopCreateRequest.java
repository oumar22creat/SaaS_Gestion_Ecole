package com.schoolsaas.transport.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record BusStopCreateRequest(@NotBlank String name, @PositiveOrZero int sequenceOrder) {
}
