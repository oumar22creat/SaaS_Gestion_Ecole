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
        int readOnlyGraceDays,

        /*
         * Tolérance après la fin d'une période réglée en espèces. Zéro par défaut : un
         * établissement non renouvelé perd l'accès le jour dit, sans quoi la date de fin
         * d'abonnement n'engagerait à rien. Le réglage existe parce qu'un règlement en
         * espèces arrive parfois avec un jour de retard, et qu'une école coupée en pleine
         * saisie de notes appelle en urgence — cet arbitrage revient à l'exploitant.
         */
        int cashGraceDays) {
}
