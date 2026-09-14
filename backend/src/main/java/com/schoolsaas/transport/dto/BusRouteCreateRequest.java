package com.schoolsaas.transport.dto;

import jakarta.validation.constraints.NotBlank;

public record BusRouteCreateRequest(@NotBlank String label) {
}
