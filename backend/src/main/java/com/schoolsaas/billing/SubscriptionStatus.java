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

    /**
     * Période payée arrivée à son terme sans renouvellement. Distinct de {@code PAST_DUE},
     * qui suppose un prélèvement refusé : un abonnement réglé en espèces n'échoue pas, il
     * cesse simplement d'être couvert au jour dit. Distinct de {@code CANCELED} aussi, que
     * seule une résiliation explicite produit — un établissement expiré est un client qu'on
     * relance, pas un client perdu.
     */
    EXPIRED,
    CANCELED
}
