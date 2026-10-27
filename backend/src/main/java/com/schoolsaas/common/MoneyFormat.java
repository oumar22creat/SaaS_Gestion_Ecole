package com.schoolsaas.common;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Formatage des montants affichés aux familles et au personnel (PDF, SMS, notifications).
 *
 * <p>Le code ISO « XOF » reste la valeur stockée et échangée (colonnes, API JSON, Stripe,
 * mobile money). Seul l'affichage le remplace par « FCFA », bien plus parlant au Mali.
 *
 * <p>XOF est une devise sans sous-unité : les montants « cents » sont des francs entiers.
 */
public final class MoneyFormat {

    /** Code ISO 4217 du franc CFA (UEMOA), tel que stocké en base et échangé avec les PSP. */
    public static final String XOF = "XOF";

    /** Libellé lisible du franc CFA, utilisé dans tous les documents et messages. */
    public static final String FCFA_LABEL = "FCFA";

    private MoneyFormat() {
    }

    /** Formate un montant en franc CFA : « 25 000 FCFA ». */
    public static String fcfa(long amountCents) {
        return format(amountCents, XOF);
    }

    /**
     * Formate un montant avec le libellé lisible de sa devise. Le séparateur de milliers est
     * une espace ordinaire : l'espace fine insécable (U+202F) produite par Java n'est pas
     * encodable par la police standard des PDF ni fiable dans un SMS.
     */
    public static String format(long amountCents, String currencyCode) {
        String formatted = NumberFormat.getIntegerInstance(Locale.FRANCE).format(amountCents)
                .replace(' ', ' ')
                .replace(' ', ' ');
        return formatted + " " + displayCurrency(currencyCode);
    }

    /** Libellé affiché d'un code devise : « XOF » (ou absent) devient « FCFA », les autres restent tels quels. */
    public static String displayCurrency(String currencyCode) {
        if (currencyCode == null || currencyCode.isBlank() || XOF.equalsIgnoreCase(currencyCode.trim())) {
            return FCFA_LABEL;
        }
        return currencyCode.trim().toUpperCase(Locale.ROOT);
    }
}
