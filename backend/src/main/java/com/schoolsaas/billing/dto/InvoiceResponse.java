package com.schoolsaas.billing.dto;

import com.schoolsaas.billing.Invoice;
import java.time.Instant;

public record InvoiceResponse(
        String status, Integer amountDue, String currency, Instant periodStart, Instant periodEnd,
        String hostedInvoiceUrl, Instant paidAt) {

    public static InvoiceResponse from(Invoice invoice) {
        return new InvoiceResponse(
                invoice.getStatus().name(),
                invoice.getAmountDue(),
                invoice.getCurrency(),
                invoice.getPeriodStart(),
                invoice.getPeriodEnd(),
                invoice.getHostedInvoiceUrl(),
                invoice.getPaidAt());
    }
}
