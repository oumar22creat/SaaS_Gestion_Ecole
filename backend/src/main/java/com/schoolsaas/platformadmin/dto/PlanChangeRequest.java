package com.schoolsaas.platformadmin.dto;

import jakarta.validation.constraints.NotNull;

public record PlanChangeRequest(@NotNull Long planId, String reason) {
}
