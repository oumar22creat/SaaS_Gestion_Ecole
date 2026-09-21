package com.schoolsaas.mobilemoney;

import com.schoolsaas.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Déclenchement d'un paiement mobile money depuis le personnel de l'établissement. Le
 * montant n'est pas un paramètre : il est calculé depuis le solde restant de la facture,
 * pour qu'un appelant ne puisse pas décider de ce qui sera encaissé.
 */
@RestController
@RequestMapping("/api/v1/school-fees/invoices/{invoiceId}/mobile-money")
public class MobileMoneyController {

    /** Numéro au format international sans le `+` (ex. 22370000000 au Mali). */
    public record InitiateRequest(
            @NotBlank @Pattern(regexp = "^[0-9]{8,15}$", message = "Numéro invalide") String payerMsisdn) {
    }

    public record InitiateResponse(Long id, String reference, String status) {
    }

    private final MobileMoneyService mobileMoneyService;

    public MobileMoneyController(MobileMoneyService mobileMoneyService) {
        this.mobileMoneyService = mobileMoneyService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'ACCOUNTANT')")
    public ApiResponse<InitiateResponse> initiate(
            @PathVariable Long invoiceId, @Valid @RequestBody InitiateRequest request) {
        MobileMoneyPayment payment = mobileMoneyService.initiate(invoiceId, request.payerMsisdn());
        return ApiResponse.of(
                new InitiateResponse(payment.getId(), payment.getReference(), payment.getStatus().name()));
    }
}
