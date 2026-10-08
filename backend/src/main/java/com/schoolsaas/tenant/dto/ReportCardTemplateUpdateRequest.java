package com.schoolsaas.tenant.dto;

import jakarta.validation.constraints.Size;

/**
 * Modèle de bulletin de l'établissement.
 *
 * <p>Les mentions officielles sont saisies en entier, libellé compris — « IA : PIKINE
 * GUEDIAWAYE », « AE : BAMAKO RIVE DROITE ». Le découpage administratif diffère d'un pays à
 * l'autre, et proposer une liste fermée reviendrait à ne servir qu'un seul système scolaire.
 *
 * <p>Tous les champs sont facultatifs : une école privée qui ne relève d'aucune académie
 * laisse les lignes vides et obtient un bulletin sans en-tête administratif, pas un bulletin
 * avec des trous.
 */
public record ReportCardTemplateUpdateRequest(
        @Size(max = 500) String reportCardHeader,
        @Size(max = 1000) String reportCardLegalMentions,
        @Size(max = 120) String officialAuthority,
        @Size(max = 120) String academyLabel,
        @Size(max = 120) String inspectionLabel,
        @Size(max = 120) String directorName,
        @Size(max = 120) String headOfficeCity,
        @Size(max = 255) String postalAddress) {
}
