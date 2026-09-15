package com.schoolsaas.billing;

import com.schoolsaas.common.ApiException;
import com.schoolsaas.tenant.TenantContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Bloque l'accès aux modules non inclus dans le plan souscrit (cahier-des-charges.md §4.1,
 * ROADMAP.md 3.7) — Cantine/Transport/Bibliothèque sont "Non" sur le plan Essentiel. Même
 * mécanisme que {@link com.schoolsaas.tenant.TenantAccessInterceptor} : une correspondance de
 * préfixe d'URI, ici vers une {@link PlanFeature} plutôt qu'un statut d'accès.
 *
 * <p>Doit s'exécuter après {@code TenantContextInterceptor} (dépend du tenant déjà résolu dans
 * {@link TenantContext}).
 */
@Component
public class PlanFeatureInterceptor implements HandlerInterceptor {

    private static final Map<String, PlanFeature> FEATURE_BY_PATH_PREFIX = Map.of(
            "/api/v1/canteen", PlanFeature.CANTEEN,
            "/api/v1/transport", PlanFeature.TRANSPORT,
            "/api/v1/library", PlanFeature.LIBRARY);

    private final PlanFeatureService planFeatureService;

    public PlanFeatureInterceptor(PlanFeatureService planFeatureService) {
        this.planFeatureService = planFeatureService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        Long tenantId = TenantContext.get();
        if (tenantId == null) {
            return true;
        }
        String uri = request.getRequestURI();
        PlanFeature requiredFeature = FEATURE_BY_PATH_PREFIX.entrySet().stream()
                .filter(entry -> uri.startsWith(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null);
        if (requiredFeature == null) {
            return true;
        }
        if (!planFeatureService.tenantHasFeature(tenantId, requiredFeature)) {
            throw ApiException.forbidden(
                    "FEATURE_NOT_INCLUDED", "Ce module n'est pas inclus dans le plan souscrit par cet établissement");
        }
        return true;
    }
}
