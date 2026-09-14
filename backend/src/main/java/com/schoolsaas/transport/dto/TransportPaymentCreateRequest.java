package com.schoolsaas.transport.dto;

import com.schoolsaas.schoolfees.FeePaymentMethod;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TransportPaymentCreateRequest(@Positive long amountCents, @NotNull FeePaymentMethod method, String reference) {
}
