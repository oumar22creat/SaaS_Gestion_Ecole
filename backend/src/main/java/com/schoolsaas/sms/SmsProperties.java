package com.schoolsaas.sms;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration de l'envoi de SMS.
 *
 * <p>La passerelle HTTP est volontairement générique : les opérateurs maliens et les
 * agrégateurs régionaux exposent tous une API REST qui accepte un destinataire et un texte,
 * mais aucun ne partage le même format. Décrire la requête en configuration évite d'écrire
 * — et de maintenir — un adaptateur par fournisseur avant de savoir lequel sera retenu.
 *
 * @param enabled     faux par défaut : sans opérateur configuré, les envois sont seulement
 *                    journalisés, jamais facturés à l'établissement
 * @param provider    nom tracé sur chaque envoi, pour le rapprochement de facture
 * @param url         point d'entrée de l'API du fournisseur
 * @param authHeader  en-tête d'authentification ({@code Authorization}, {@code X-API-Key}…)
 * @param authValue   valeur de cet en-tête — un secret, jamais commité
 * @param bodyTemplate corps JSON envoyé, où {@code {to}} et {@code {text}} sont remplacés
 * @param senderName  expéditeur affiché sur le téléphone, quand l'opérateur l'autorise
 * @param defaultCountryCode indicatif ajouté aux numéros saisis en national (Mali : +223)
 */
@ConfigurationProperties(prefix = "app.sms")
public record SmsProperties(
        boolean enabled,
        String provider,
        String url,
        String authHeader,
        String authValue,
        String bodyTemplate,
        String senderName,
        String defaultCountryCode) {
}
