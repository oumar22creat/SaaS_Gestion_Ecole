package com.schoolsaas.platformadmin.dto;

import com.schoolsaas.billing.SubscriptionStatus;
import com.schoolsaas.tenant.TenantStatus;
import java.time.Instant;

/**
 * Un établissement vu depuis la console plateforme.
 *
 * <p>Volontairement dépourvu de tout effectif : les tables élèves, parents et utilisateurs
 * sont protégées par Row-Level Security et restent invisibles à l'opérateur de la plateforme
 * (docs/ARCHITECTURE.md ADR-001). Ce n'est pas une limite à contourner mais une garantie à
 * tenir — un éditeur de logiciel n'a pas à lire le dossier scolaire des élèves de ses clients.
 */
public record TenantAdminResponse(
        Long id,
        String name,
        String subdomain,
        String customDomain,
        TenantStatus status,
        Instant createdAt,
        String planCode,
        String planName,
        Integer planPriceCents,
        String currency,
        SubscriptionStatus subscriptionStatus,
        Instant trialEndsAt,
        Instant currentPeriodEnd,
        Instant paymentFailedAt) {
}
