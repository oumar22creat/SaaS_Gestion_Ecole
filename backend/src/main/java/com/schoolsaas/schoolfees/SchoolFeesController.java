package com.schoolsaas.schoolfees;

import com.schoolsaas.auth.AuthenticatedPrincipal;
import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.schoolfees.dto.FeePaymentCreateRequest;
import com.schoolsaas.schoolfees.dto.FeePaymentResponse;
import com.schoolsaas.schoolfees.dto.FeeReportingResponse;
import com.schoolsaas.schoolfees.dto.FeeScheduleCreateRequest;
import com.schoolsaas.schoolfees.dto.FeeScheduleResponse;
import com.schoolsaas.schoolfees.dto.StudentFeeInvoiceResponse;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Comptabilité et frais scolaires — cahier-des-charges.md §19.4, ROADMAP.md 3.3. */
@RestController
@RequestMapping("/api/v1/school-fees")
@PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'ACCOUNTANT')")
public class SchoolFeesController {

    private final SchoolFeesService schoolFeesService;

    public SchoolFeesController(SchoolFeesService schoolFeesService) {
        this.schoolFeesService = schoolFeesService;
    }

    @PostMapping("/schedules")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ApiResponse<FeeScheduleResponse> createSchedule(@Valid @RequestBody FeeScheduleCreateRequest request) {
        return ApiResponse.of(FeeScheduleResponse.from(schoolFeesService.createSchedule(request)));
    }

    @GetMapping("/schedules")
    public ApiResponse<List<FeeScheduleResponse>> listSchedules(@RequestParam Long schoolClassId) {
        List<FeeScheduleResponse> data =
                schoolFeesService.listSchedulesForClass(schoolClassId).stream().map(FeeScheduleResponse::from).toList();
        return ApiResponse.of(data);
    }

    @PostMapping("/schedules/{id}/generate-invoices")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ApiResponse<List<StudentFeeInvoiceResponse>> generateInvoices(@PathVariable Long id) {
        List<StudentFeeInvoiceResponse> data = schoolFeesService.generateInvoices(id).stream()
                .map(invoice -> StudentFeeInvoiceResponse.from(invoice, schoolFeesService.paidAmountForInvoice(invoice.getId())))
                .toList();
        return ApiResponse.of(data);
    }

    @GetMapping("/invoices/{id}")
    public ApiResponse<StudentFeeInvoiceResponse> getInvoice(@PathVariable Long id) {
        StudentFeeInvoice invoice = schoolFeesService.getInvoice(id);
        return ApiResponse.of(StudentFeeInvoiceResponse.from(invoice, schoolFeesService.paidAmountForInvoice(id)));
    }

    @GetMapping("/students/{studentId}/invoices")
    public ApiResponse<List<StudentFeeInvoiceResponse>> invoicesForStudent(@PathVariable Long studentId) {
        List<StudentFeeInvoiceResponse> data = schoolFeesService.invoicesForStudent(studentId).stream()
                .map(invoice -> StudentFeeInvoiceResponse.from(invoice, schoolFeesService.paidAmountForInvoice(invoice.getId())))
                .toList();
        return ApiResponse.of(data);
    }

    @PostMapping("/invoices/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ApiResponse<FeePaymentResponse> recordPayment(@PathVariable Long id, @Valid @RequestBody FeePaymentCreateRequest request) {
        return ApiResponse.of(FeePaymentResponse.from(schoolFeesService.recordPayment(id, request, currentUserId())));
    }

    @GetMapping("/invoices/{id}/payments")
    public ApiResponse<List<FeePaymentResponse>> paymentsForInvoice(@PathVariable Long id) {
        List<FeePaymentResponse> data = schoolFeesService.paymentsForInvoice(id).stream().map(FeePaymentResponse::from).toList();
        return ApiResponse.of(data);
    }

    @GetMapping("/reporting")
    public ApiResponse<FeeReportingResponse> reporting(
            @RequestParam Long schoolClassId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.of(schoolFeesService.reporting(schoolClassId, from, to));
    }

    private Long currentUserId() {
        return ((AuthenticatedPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).subjectId();
    }
}
