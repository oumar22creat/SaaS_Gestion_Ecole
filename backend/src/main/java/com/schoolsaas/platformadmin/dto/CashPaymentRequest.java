package com.schoolsaas.platformadmin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Règlement encaissé en espèces, enregistré par le Super-Administrateur pour ouvrir ou
 * prolonger l'abonnement d'un établissement.
 *
 * <p>La plateforme n'encaisse rien elle-même aujourd'hui : l'argent change de main hors
 * application, et cet enregistrement est la seule trace de ce que l'éditeur doit en
 * contrepartie. C'est pourquoi la durée est exprimée en mois — ce qu'on annonce à
 * l'établissement — et non en une date de fin saisie à la main, qu'une faute de frappe
 * rendrait fausse sans que rien ne le signale.
 *
 * <p>Plafond de 36 mois : au-delà, il ne s'agit plus d'un règlement mais d'un contrat
 * pluriannuel, qui mérite d'être discuté avant d'être saisi.
 */
public record CashPaymentRequest(@NotNull Long planId, @Positive @Max(36) int months, String reason) {
}
