package com.schoolsaas.billing;

/**
 * État de l'abonnement côté facturation (piloté par Stripe). Distinct de
 * {@link com.schoolsaas.tenant.TenantStatus}, qui pilote l'accès applicatif : voir
 * docs/ARCHITECTURE.md ADR-009 pour la correspondance entre les deux.
 */
public enum SubscriptionStatus {
    TRIALING,
    ACTIVE,
    PAST_DUE,
    CANCELED
}
