package com.schoolsaas.tenant;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Résout le contexte tenant courant (si le filtre d'authentification JWT ne l'a pas déjà fait
 * à partir de la revendication {@code tenantId} du token) puis délègue à
 * {@link TenantSessionConfigurer} l'activation du filtre Hibernate global et de la variable de
 * session PostgreSQL utilisée par les politiques Row-Level Security (voir
 * docs/ARCHITECTURE.md ADR-001).
 *
 * <p>S'exécute en tant que {@link HandlerInterceptor} (dans le dispatch MVC) plutôt qu'en tant
 * que {@code Filter} servlet brut : cela garantit qu'il s'exécute après l'ouverture de
 * l'EntityManager par {@code OpenEntityManagerInViewFilter}, sans avoir à jongler avec l'ordre
 * des filtres Spring Boot.
 */
@Component
public class TenantContextInterceptor implements HandlerInterceptor {

    private final TenantResolver tenantResolver;
    private final TenantSessionConfigurer tenantSessionConfigurer;

    public TenantContextInterceptor(TenantResolver tenantResolver, TenantSessionConfigurer tenantSessionConfigurer) {
        this.tenantResolver = tenantResolver;
        this.tenantSessionConfigurer = tenantSessionConfigurer;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (TenantContext.get() == null) {
            tenantResolver.resolve(request).ifPresent(tenant -> TenantContext.set(tenant.getId()));
        }

        Long tenantId = TenantContext.get();
        if (tenantId != null) {
            tenantSessionConfigurer.applyTenant(tenantId);
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
    }
}
