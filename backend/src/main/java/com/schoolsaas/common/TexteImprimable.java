package com.schoolsaas.common;

/**
 * Ramène un texte à ce que les polices Standard 14 de PDFBox savent encoder.
 *
 * <p>Ces polices utilisent WinAnsi. Un seul caractère hors de ce jeu fait échouer la
 * génération du document entier — pas la ligne, le document. Un nom à l'orthographe
 * inattendue, une apostrophe typographique collée depuis un traitement de texte, un tiret
 * cadratin dans un intitulé de période : chacun suffirait à empêcher d'imprimer la liasse
 * d'une classe entière, et l'erreur remontée ne désignerait pas le coupable.
 *
 * <p>La ponctuation typographique est convertie plutôt que remplacée par un point
 * d'interrogation : « BULLETIN DE NOTES — SEMESTRE 1 » doit s'imprimer avec un tiret, pas
 * avec un « ? » au milieu du titre.
 */
public final class TexteImprimable {

    private TexteImprimable() {
    }

    public static String nettoyer(String texte) {
        if (texte == null) {
            return "";
        }
        StringBuilder propre = new StringBuilder(texte.length());
        for (char caractere : texte.toCharArray()) {
            switch (caractere) {
                case '‘', '’' -> propre.append('\'');
                case '“', '”' -> propre.append('"');
                case '‐', '‑', '‒', '–', '—' -> propre.append('-');
                case ' ', ' ', ' ' -> propre.append(' ');
                case '…' -> propre.append("...");
                case 'Œ' -> propre.append("OE");
                case 'œ' -> propre.append("oe");
                case '€' -> propre.append("EUR");
                default -> {
                    // ASCII imprimable et Latin-1 : tous les accents du français y figurent.
                    if ((caractere >= 0x20 && caractere <= 0x7E) || (caractere >= 0xA0 && caractere <= 0xFF)) {
                        propre.append(caractere);
                    } else {
                        propre.append('?');
                    }
                }
            }
        }
        return propre.toString();
    }
}
