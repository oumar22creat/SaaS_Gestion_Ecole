package com.schoolsaas.transport;

import com.schoolsaas.auth.AuthenticatedPrincipal;
import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.transport.dto.BusRouteCreateRequest;
import com.schoolsaas.transport.dto.BusRouteResponse;
import com.schoolsaas.transport.dto.BusStopCreateRequest;
import com.schoolsaas.transport.dto.BusStopResponse;
import com.schoolsaas.transport.dto.TransportAssignmentRequest;
import com.schoolsaas.transport.dto.TransportAssignmentResponse;
import com.schoolsaas.transport.dto.TransportInvoiceGenerateRequest;
import com.schoolsaas.transport.dto.TransportInvoiceResponse;
import com.schoolsaas.transport.dto.TransportPaymentCreateRequest;
import com.schoolsaas.transport.dto.TransportPaymentResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Transport scolaire — cahier-des-charges.md §19.2, ROADMAP.md 3.6. */
@RestController
@RequestMapping("/api/v1/transport")
@PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'SECRETARY', 'ACCOUNTANT')")
public class TransportController {

    private final TransportService transportService;

    public TransportController(TransportService transportService) {
        this.transportService = transportService;
    }

    @PostMapping("/routes")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'SECRETARY')")
    public ApiResponse<BusRouteResponse> createRoute(@Valid @RequestBody BusRouteCreateRequest request) {
        return ApiResponse.of(BusRouteResponse.from(transportService.createRoute(request)));
    }

    @GetMapping("/routes")
    public ApiResponse<List<BusRouteResponse>> listRoutes() {
        return ApiResponse.of(transportService.listRoutes().stream().map(BusRouteResponse::from).toList());
    }

    @PostMapping("/routes/{id}/stops")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'SECRETARY')")
    public ApiResponse<BusStopResponse> addStop(@PathVariable Long id, @Valid @RequestBody BusStopCreateRequest request) {
        return ApiResponse.of(BusStopResponse.from(transportService.addStop(id, request)));
    }

    @GetMapping("/routes/{id}/stops")
    public ApiResponse<List<BusStopResponse>> listStops(@PathVariable Long id) {
        return ApiResponse.of(transportService.listStops(id).stream().map(BusStopResponse::from).toList());
    }

    @PostMapping("/assignments")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'SECRETARY')")
    public ApiResponse<TransportAssignmentResponse> assignStudent(@Valid @RequestBody TransportAssignmentRequest request) {
        return ApiResponse.of(TransportAssignmentResponse.from(transportService.assignStudent(request)));
    }

    @GetMapping("/students/{studentId}/assignment")
    public ApiResponse<TransportAssignmentResponse> getAssignment(@PathVariable Long studentId) {
        return ApiResponse.of(TransportAssignmentResponse.from(transportService.getAssignmentForStudent(studentId)));
    }

    @PostMapping("/invoices/generate")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ApiResponse<TransportInvoiceResponse> generateInvoice(@Valid @RequestBody TransportInvoiceGenerateRequest request) {
        TransportInvoice invoice = transportService.generateInvoice(request);
        return ApiResponse.of(TransportInvoiceResponse.from(invoice, transportService.paidAmountForInvoice(invoice.getId())));
    }

    @GetMapping("/invoices/{id}")
    public ApiResponse<TransportInvoiceResponse> getInvoice(@PathVariable Long id) {
        TransportInvoice invoice = transportService.getInvoice(id);
        return ApiResponse.of(TransportInvoiceResponse.from(invoice, transportService.paidAmountForInvoice(id)));
    }

    @GetMapping("/students/{studentId}/invoices")
    public ApiResponse<List<TransportInvoiceResponse>> invoicesForStudent(@PathVariable Long studentId) {
        List<TransportInvoiceResponse> data = transportService.invoicesForStudent(studentId).stream()
                .map(invoice -> TransportInvoiceResponse.from(invoice, transportService.paidAmountForInvoice(invoice.getId())))
                .toList();
        return ApiResponse.of(data);
    }

    @PostMapping("/invoices/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ApiResponse<TransportPaymentResponse> recordPayment(@PathVariable Long id, @Valid @RequestBody TransportPaymentCreateRequest request) {
        return ApiResponse.of(TransportPaymentResponse.from(transportService.recordPayment(id, request, currentUserId())));
    }

    @GetMapping("/invoices/{id}/payments")
    public ApiResponse<List<TransportPaymentResponse>> paymentsForInvoice(@PathVariable Long id) {
        return ApiResponse.of(transportService.paymentsForInvoice(id).stream().map(TransportPaymentResponse::from).toList());
    }

    private Long currentUserId() {
        return ((AuthenticatedPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).subjectId();
    }
}
