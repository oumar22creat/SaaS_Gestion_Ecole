package com.schoolsaas.schoolfees.dto;

import java.time.LocalDate;
import java.util.List;

/** Reporting financier consolidé pour Direction/Comptable (cahier-des-charges.md §19.4). */
public record FeeReportingResponse(
        Long schoolClassId,
        LocalDate periodFrom,
        LocalDate periodTo,
        long totalDueCents,
        long totalPaidCents,
        long totalOutstandingCents,
        List<StudentFeeInvoiceResponse> unpaidInvoices) {
}
