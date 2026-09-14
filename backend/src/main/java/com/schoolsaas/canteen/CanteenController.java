package com.schoolsaas.canteen;

import com.schoolsaas.auth.AuthenticatedPrincipal;
import com.schoolsaas.canteen.dto.CanteenInvoiceGenerateRequest;
import com.schoolsaas.canteen.dto.CanteenInvoiceResponse;
import com.schoolsaas.canteen.dto.CanteenPaymentCreateRequest;
import com.schoolsaas.canteen.dto.CanteenPaymentResponse;
import com.schoolsaas.canteen.dto.MealReservationCreateRequest;
import com.schoolsaas.canteen.dto.MealReservationResponse;
import com.schoolsaas.canteen.dto.MenuCreateRequest;
import com.schoolsaas.canteen.dto.MenuResponse;
import com.schoolsaas.common.ApiResponse;
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

/** Cantine — cahier-des-charges.md §19.1, ROADMAP.md 3.4. */
@RestController
@RequestMapping("/api/v1/canteen")
@PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'SECRETARY', 'ACCOUNTANT')")
public class CanteenController {

    private final CanteenService canteenService;

    public CanteenController(CanteenService canteenService) {
        this.canteenService = canteenService;
    }

    @PostMapping("/menus")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'SECRETARY')")
    public ApiResponse<MenuResponse> upsertMenu(@Valid @RequestBody MenuCreateRequest request) {
        return ApiResponse.of(MenuResponse.from(canteenService.upsertMenu(request)));
    }

    @GetMapping("/menus")
    public ApiResponse<List<MenuResponse>> listMenus(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<MenuResponse> data = canteenService.listMenus(from, to).stream().map(MenuResponse::from).toList();
        return ApiResponse.of(data);
    }

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'SECRETARY')")
    public ApiResponse<MealReservationResponse> reserveMeal(@Valid @RequestBody MealReservationCreateRequest request) {
        return ApiResponse.of(MealReservationResponse.from(canteenService.reserveMeal(request, currentUserId())));
    }

    @GetMapping("/students/{studentId}/reservations")
    public ApiResponse<List<MealReservationResponse>> reservationsForStudent(
            @PathVariable Long studentId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<MealReservationResponse> data =
                canteenService.reservationsForStudent(studentId, from, to).stream().map(MealReservationResponse::from).toList();
        return ApiResponse.of(data);
    }

    @PostMapping("/invoices/generate")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ApiResponse<CanteenInvoiceResponse> generateInvoice(@Valid @RequestBody CanteenInvoiceGenerateRequest request) {
        CanteenInvoice invoice = canteenService.generateInvoice(request);
        return ApiResponse.of(CanteenInvoiceResponse.from(invoice, canteenService.paidAmountForInvoice(invoice.getId())));
    }

    @GetMapping("/invoices/{id}")
    public ApiResponse<CanteenInvoiceResponse> getInvoice(@PathVariable Long id) {
        CanteenInvoice invoice = canteenService.getInvoice(id);
        return ApiResponse.of(CanteenInvoiceResponse.from(invoice, canteenService.paidAmountForInvoice(id)));
    }

    @GetMapping("/students/{studentId}/invoices")
    public ApiResponse<List<CanteenInvoiceResponse>> invoicesForStudent(@PathVariable Long studentId) {
        List<CanteenInvoiceResponse> data = canteenService.invoicesForStudent(studentId).stream()
                .map(invoice -> CanteenInvoiceResponse.from(invoice, canteenService.paidAmountForInvoice(invoice.getId())))
                .toList();
        return ApiResponse.of(data);
    }

    @GetMapping("/invoices/unpaid")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'ACCOUNTANT')")
    public ApiResponse<List<CanteenInvoiceResponse>> unpaidInvoices() {
        List<CanteenInvoiceResponse> data = canteenService.listUnpaidInvoices().stream()
                .map(invoice -> CanteenInvoiceResponse.from(invoice, canteenService.paidAmountForInvoice(invoice.getId())))
                .toList();
        return ApiResponse.of(data);
    }

    @PostMapping("/invoices/{id}/payments")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'ACCOUNTANT')")
    public ApiResponse<CanteenPaymentResponse> recordPayment(@PathVariable Long id, @Valid @RequestBody CanteenPaymentCreateRequest request) {
        return ApiResponse.of(CanteenPaymentResponse.from(canteenService.recordPayment(id, request, currentUserId())));
    }

    @GetMapping("/invoices/{id}/payments")
    public ApiResponse<List<CanteenPaymentResponse>> paymentsForInvoice(@PathVariable Long id) {
        List<CanteenPaymentResponse> data = canteenService.paymentsForInvoice(id).stream().map(CanteenPaymentResponse::from).toList();
        return ApiResponse.of(data);
    }

    private Long currentUserId() {
        return ((AuthenticatedPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).subjectId();
    }
}
