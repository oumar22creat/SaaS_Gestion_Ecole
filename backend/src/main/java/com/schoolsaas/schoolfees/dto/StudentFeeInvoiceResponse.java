package com.schoolsaas.schoolfees.dto;

import com.schoolsaas.schoolfees.FeeInvoiceStatus;
import com.schoolsaas.schoolfees.StudentFeeInvoice;
import java.time.Instant;

public record StudentFeeInvoiceResponse(
        Long id,
        Long feeScheduleId,
        Long studentId,
        long amountDueCents,
        long amountPaidCents,
        FeeInvoiceStatus status,
        Instant issuedAt) {

    public static StudentFeeInvoiceResponse from(StudentFeeInvoice invoice, long amountPaidCents) {
        return new StudentFeeInvoiceResponse(
                invoice.getId(), invoice.getFeeScheduleId(), invoice.getStudentId(), invoice.getAmountDueCents(),
                amountPaidCents, invoice.getStatus(), invoice.getIssuedAt());
    }
}
