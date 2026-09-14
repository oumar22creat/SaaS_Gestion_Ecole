package com.schoolsaas.transport.dto;

import com.schoolsaas.transport.TransportInvoice;
import com.schoolsaas.transport.TransportInvoiceStatus;
import java.time.LocalDate;

public record TransportInvoiceResponse(
        Long id,
        Long studentId,
        LocalDate periodFrom,
        LocalDate periodTo,
        long amountDueCents,
        long amountPaidCents,
        TransportInvoiceStatus status) {

    public static TransportInvoiceResponse from(TransportInvoice invoice, long amountPaidCents) {
        return new TransportInvoiceResponse(
                invoice.getId(), invoice.getStudentId(), invoice.getPeriodFrom(), invoice.getPeriodTo(),
                invoice.getAmountDueCents(), amountPaidCents, invoice.getStatus());
    }
}
