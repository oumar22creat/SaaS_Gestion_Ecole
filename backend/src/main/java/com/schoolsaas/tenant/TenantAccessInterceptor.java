package com.schoolsaas.tenant;

import com.schoolsaas.common.ApiException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Applique le blocage progressif d'accès porté par {@link TenantStatus} (cahier-des-
 * charges.md §4.2, voir docs/ARCHITECTURE.md ADR-009) : {@code READ_ONLY} bloque les
 * écritures, {@code SUSPENDED}/{@code CANCELLED} bloquent tout — sauf le petit nombre de
 * routes nécessaires pour que l'établissement puisse encore se connecter et payer pour
 * réactiver son accès.
 *
 * <p>Doit s'exécuter APRÈS {@link TenantContextInterceptor} (même raison d'ordre MVC, voir
 * {@link TenantWebConfig}) puisqu'il dépend du tenant déjà résolu dans {@link TenantContext}.
 */
@Component
public class TenantAccessInterceptor implements HandlerInterceptor {

    private static final List<String> ALLOWED_PATH_PREFIXES =
            List.of("/api/v1/auth/", "/api/v1/billing/", "/api/v1/tenants/register");
    private static final Set<String> SAFE_METHODS = Set.of("GET", "HEAD", "OPTIONS");

    private final TenantRepository tenantRepository;

    public TenantAccessInterceptor(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Long tenantId = TenantContext.get();
        if (tenantId == null || isAllowedPath(request.getRequestURI())) {
            return true;
        }

        TenantStatus status = tenantRepository.findById(tenantId).map(Tenant::getStatus).orElse(null);
        if (status == TenantStatus.SUSPENDED || status == TenantStatus.CANCELLED) {
            throw ApiException.forbidden("TENANT_SUSPENDED", "Accès suspendu : abonnement à régulariser");
        }
        if (status == TenantStatus.READ_ONLY && !SAFE_METHODS.contains(request.getMethod())) {
            throw ApiException.forbidden("TENANT_READ_ONLY", "Accès en lecture seule : abonnement à régulariser");
        }
        return true;
    }

    private boolean isAllowedPath(String requestUri) {
        return ALLOWED_PATH_PREFIXES.stream().anyMatch(requestUri::startsWith);
    }
}
