package com.schoolsaas.tenant;

/**
 * Contexte tenant courant, propagé sur le thread de la requête en cours (voir
 * CLAUDE.md règle 2 et docs/ARCHITECTURE.md ADR-001). Positionné par
 * {@link TenantContextInterceptor} avant que la requête n'atteigne un contrôleur, effacé à
 * la fin de la requête pour éviter toute fuite entre deux requêtes sur un thread réutilisé.
 */
public final class TenantContext {

    private static final ThreadLocal<Long> CURRENT_TENANT_ID = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(Long tenantId) {
        CURRENT_TENANT_ID.set(tenantId);
    }

    public static Long get() {
        return CURRENT_TENANT_ID.get();
    }

    public static void clear() {
        CURRENT_TENANT_ID.remove();
    }
}
