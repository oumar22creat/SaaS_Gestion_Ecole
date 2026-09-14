package com.schoolsaas.transport.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;

public record TransportInvoiceGenerateRequest(
        @NotNull Long studentId, @NotNull LocalDate periodFrom, @NotNull LocalDate periodTo, @Positive long amountDueCents) {
}
