package com.schoolsaas.schoolfees.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

/**
 * Remise accordée sur une facture : bourse, tarif fratrie, exonération.
 *
 * @param amountCents réduction en francs entiers. Zéro annule une remise accordée par
 *                    erreur ; la facture revient au montant de l'échéancier.
 * @param reason      obligatoire. Une réduction sans motif est un trou dans la comptabilité :
 *                    personne, six mois plus tard, ne saura si c'était une bourse, une
 *                    erreur de saisie ou un arrangement non autorisé. Le motif s'imprime sur
 *                    le reçu et se lit dans le portail famille.
 */
public record FeeDiscountRequest(
        @PositiveOrZero long amountCents,
        @NotBlank @Size(max = 255) String reason) {
}
