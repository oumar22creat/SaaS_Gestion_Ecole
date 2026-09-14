package com.schoolsaas.library.dto;

import jakarta.validation.constraints.NotNull;

public record BookReservationCreateRequest(@NotNull Long studentId) {
}
