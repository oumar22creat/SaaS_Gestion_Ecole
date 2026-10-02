package com.schoolsaas.notification;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Réglages de l'envoi des notifications push (docs/NOTIFICATIONS-PUSH.md).
 *
 * <p>Désactivé par défaut, comme le SMS et pour la même raison : rien ne part tant que
 * l'exploitant n'a pas déposé ses identifiants et décidé d'ouvrir le canal. La passerelle
 * journalisante prend alors le relais.
 */
@ConfigurationProperties(prefix = "app.fcm")
public record FcmProperties(
        boolean enabled,

        /**
         * Chemin du fichier de compte de service Firebase. Un chemin et non le contenu : la
         * clé est un document JSON de plusieurs lignes, qu'une variable d'environnement
         * rendrait illisible et exposerait dans la sortie de {@code docker inspect}.
         */
        String credentialsPath) {
}
