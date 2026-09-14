package com.schoolsaas.billing.dto;

import jakarta.validation.constraints.NotBlank;

public record CheckoutRequest(@NotBlank String planCode) {
}
