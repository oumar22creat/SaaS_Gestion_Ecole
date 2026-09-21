package com.schoolsaas.common.ratelimit;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Seuils de limitation de débit (cahier-des-charges.md §24.1).
 *
 * <p>Valeurs volontairement basses : ces deux routes ne sont pas des parcours répétitifs.
 * Un utilisateur légitime se connecte une fois, un établissement s'inscrit une fois.
 */
@ConfigurationProperties(prefix = "app.rate-limit")
public record RateLimitProperties(
        boolean enabled,
        /** Tentatives de connexion tolérées par adresse IP sur la fenêtre. */
        int loginPerIp,
        /** Tentatives tolérées sur un même compte, quelle que soit l'origine. */
        int loginPerAccount,
        /** Inscriptions d'établissement tolérées par adresse IP. */
        int registrationPerIp,
        Duration window) {
}
