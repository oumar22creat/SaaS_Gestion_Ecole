package com.schoolsaas.common;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

/**
 * Les échecs d'authentification/autorisation sont interceptés par la chaîne de filtres Spring
 * Security, avant le {@code DispatcherServlet} : {@link GlobalExceptionHandler} ne peut pas les
 * voir. Cette classe garantit malgré tout la même enveloppe d'erreur JSON uniforme (voir
 * docs/API_CONVENTIONS.md) pour les codes 401 et 403.
 */
@Component
public class RestSecurityHandlers implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    public RestSecurityHandlers(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException)
            throws IOException {
        writeError(response, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Authentification requise");
    }

    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException)
            throws IOException {
        writeError(response, HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Accès refusé");
    }

    private void writeError(HttpServletResponse response, HttpStatus status, String code, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        var body = new GlobalExceptionHandler.ErrorBody(new GlobalExceptionHandler.ErrorDetail(code, message, List.of()));
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}
