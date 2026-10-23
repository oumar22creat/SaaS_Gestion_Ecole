package com.schoolsaas.common;

import java.util.Locale;

/**
 * Forme canonique d'une adresse e-mail : sans espaces autour, en minuscules.
 *
 * <p>Appliquée à l'enregistrement des comptes ({@code users}) et des fiches enseignant
 * ({@code teachers}), et à toute comparaison entre les deux (ADR-030) : « Prof@Ecole.ml »
 * saisi d'un côté et « prof@ecole.ml » de l'autre désignent la même personne. Les lignes
 * existantes ne sont pas réécrites (pas de migration de données) ; les comparaisons, elles,
 * normalisent les deux côtés.
 */
public final class EmailNormalizer {

    private EmailNormalizer() {
    }

    /** Adresse normalisée, ou {@code null} si absente ou vide. */
    public static String normalize(String email) {
        if (email == null) {
            return null;
        }
        String trimmed = email.trim();
        return trimmed.isEmpty() ? null : trimmed.toLowerCase(Locale.ROOT);
    }
}
