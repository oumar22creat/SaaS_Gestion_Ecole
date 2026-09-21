package com.schoolsaas.mobilemoney;

import com.schoolsaas.common.ApiException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Retour de l'opérateur après validation (ou refus) du paiement par le payeur.
 *
 * <p>Cette route est publique : l'opérateur n'a ni compte ni jeton chez nous. Elle est donc
 * protégée par un secret partagé, comparé en temps constant, et elle <b>échoue fermée</b> :
 * si aucun secret n'est configuré, tout appel est rejeté. L'alternative — accepter les
 * callbacks quand la configuration est absente — laisserait n'importe qui marquer une facture
 * comme payée.
 */
@RestController
@RequestMapping("/api/v1/payments/mobile-money")
public class MobileMoneyCallbackController {

    public record CallbackRequest(
            @NotBlank String reference,
            @NotNull Boolean succeeded,
            String providerReference,
            String failureReason) {
    }

    private final MobileMoneyService mobileMoneyService;
    private final String callbackSecret;

    public MobileMoneyCallbackController(
            MobileMoneyService mobileMoneyService,
            @Value("${app.mobile-money.callback-secret:}") String callbackSecret) {
        this.mobileMoneyService = mobileMoneyService;
        this.callbackSecret = callbackSecret;
    }

    @PostMapping("/callback")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void callback(
            @RequestHeader(value = "X-Callback-Secret", required = false) String providedSecret,
            @Valid @RequestBody CallbackRequest request) {
        requireValidSecret(providedSecret);
        mobileMoneyService.handleCallback(
                request.reference(), request.succeeded(), request.providerReference(), request.failureReason());
    }

    private void requireValidSecret(String provided) {
        if (callbackSecret == null || callbackSecret.isBlank()) {
            throw ApiException.forbidden(
                    "CALLBACK_NOT_CONFIGURED", "Le callback mobile money n'est pas configuré");
        }
        byte[] expected = callbackSecret.getBytes(StandardCharsets.UTF_8);
        byte[] actual = provided == null ? new byte[0] : provided.getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(expected, actual)) {
            throw ApiException.forbidden("INVALID_CALLBACK_SECRET", "Secret de callback invalide");
        }
    }
}
