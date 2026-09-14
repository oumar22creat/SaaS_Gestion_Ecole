package com.schoolsaas.statistics.dto;

/**
 * Statistiques plateforme réservées au Super-Administrateur — cahier-des-charges.md §5/§18,
 * ROADMAP.md 2.6. {@code churnRate}/{@code conversionRate} sont des taux cumulés depuis
 * l'origine (aucun historique d'événements d'abonnement n'est conservé pour calculer des
 * cohortes par période, voir docs/ARCHITECTURE.md ADR-021).
 */
public record PlatformDashboardSummaryResponse(
        long trialCount,
        long activeCount,
        long readOnlyCount,
        long suspendedCount,
        long cancelledCount,
        long mrrCents,
        long arrCents,
        String currency,
        Double churnRate,
        Double conversionRate) {
}
