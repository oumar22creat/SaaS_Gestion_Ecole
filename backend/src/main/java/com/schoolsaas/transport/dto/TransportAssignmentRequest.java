package com.schoolsaas.transport.dto;

import jakarta.validation.constraints.NotNull;

public record TransportAssignmentRequest(@NotNull Long studentId, @NotNull Long busRouteId, @NotNull Long busStopId) {
}
