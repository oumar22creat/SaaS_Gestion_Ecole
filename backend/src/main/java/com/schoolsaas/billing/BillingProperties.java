package com.schoolsaas.billing;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Paramètres du cycle de vie de l'abonnement (cahier-des-charges.md §4.2). Valeurs par
 * défaut choisies comme point de départ raisonnable, à confirmer avec le porteur de projet —
 * voir docs/ARCHITECTURE.md ADR-009 (même traitement que la grille tarifaire de V4).
 */
@ConfigurationProperties(prefix = "app.billing")
public record BillingProperties(
        int trialDays,
        String defaultTrialPlanCode,
        int pastDueGraceDays,
        int readOnlyGraceDays) {
}
