package com.schoolsaas.common;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Traduit toute exception en enveloppe d'erreur uniforme — voir docs/API_CONVENTIONS.md. */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    public record ErrorBody(ErrorDetail error) {
    }

    public record ErrorDetail(String code, String message, List<String> details) {
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorBody> handleApiException(ApiException ex) {
        return ResponseEntity.status(ex.getStatus())
                .body(new ErrorBody(new ErrorDetail(ex.getCode(), ex.getMessage(), ex.getDetails())));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorBody> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fieldError -> fieldError.getField() + ": " + fieldError.getDefaultMessage())
                .toList();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorBody(new ErrorDetail("VALIDATION_ERROR", "Requête invalide", details)));
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorBody> handleBadCredentials(BadCredentialsException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorBody(new ErrorDetail("INVALID_CREDENTIALS", "Identifiants invalides", List.of())));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorBody> handleAccessDenied(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(new ErrorBody(new ErrorDetail("ACCESS_DENIED", "Accès refusé", List.of())));
    }

    /**
     * Aucun endpoint ne correspond à l'URL demandée (voir docs/API_CONVENTIONS.md : 404
     * réservé aux ressources introuvables). Sans ce handler dédié, cette exception — levée
     * par le gestionnaire de ressources statiques de Spring en repli quand aucun
     * {@code @RequestMapping} ne correspond — tombait dans {@link #handleUnexpected} et
     * renvoyait à tort 500 (découvert en testant le frontend contre un vrai backend : un
     * endpoint jamais implémenté, ex. `/api/v1/tenants/current/branding`, silencieusement
     * "cassait" au lieu de simplement répondre 404).
     */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorBody> handleNoResourceFound(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorBody(new ErrorDetail("NOT_FOUND", "Ressource introuvable", List.of())));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorBody> handleUnexpected(Exception ex) {
        log.error("Erreur interne non gérée", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorBody(new ErrorDetail("INTERNAL_ERROR", "Erreur interne", List.of())));
    }
}
