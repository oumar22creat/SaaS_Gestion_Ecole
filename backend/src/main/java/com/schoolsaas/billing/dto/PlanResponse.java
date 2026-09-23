package com.schoolsaas.billing.dto;

import com.schoolsaas.billing.Plan;

/**
 * Un plan tel qu'un établissement le lit sur l'écran Abonnement.
 *
 * <p>{@code annualPriceCents} est dérivé du mensuel plutôt que stocké : l'offre publique
 * annonce un tarif annuel qui vaut exactement douze fois le mensuel, et une seconde colonne
 * finirait par diverger de la première sans que rien ne le signale.
 *
 * <p>Les modules inclus accompagnent le prix. Sans eux, l'écran demandait de choisir un plan
 * sans dire ce qu'il apporte — le site vitrine, lui, le détaille.
 */
public record PlanResponse(
        String code,
        String name,
        Integer priceCents,
        Integer annualPriceCents,
        String currency,
        Integer maxStudents,
        boolean canteenIncluded,
        boolean transportIncluded,
        boolean libraryIncluded,
        boolean customDomainIncluded,
        boolean purchasable) {

    public static PlanResponse from(Plan plan) {
        return new PlanResponse(
                plan.getCode(),
                plan.getName(),
                plan.getPriceCents(),
                plan.getPriceCents() == null ? null : plan.getPriceCents() * 12,
                plan.getCurrency(),
                plan.getMaxStudents(),
                plan.isCanteenIncluded(),
                plan.isTransportIncluded(),
                plan.isLibraryIncluded(),
                plan.isCustomDomainIncluded(),
                plan.getStripePriceId() != null);
    }
}
