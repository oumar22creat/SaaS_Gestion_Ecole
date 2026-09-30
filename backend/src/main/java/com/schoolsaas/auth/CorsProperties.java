package com.schoolsaas.auth;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Origines autorisées à appeler l'API depuis un navigateur (voir docs/ARCHITECTURE.md).
 *
 * <p>Les valeurs sont des MOTIFS, pas des origines exactes : {@code https://*.exemple.com}
 * couvre l'ensemble des établissements, qui vivent chacun sur leur sous-domaine.
 *
 * <p>Contrairement à ce qui était écrit ici, la production est concernée. Les navigateurs
 * joignent un en-tête {@code Origin} aux POST même en même-origine, et Spring considère alors
 * la requête comme une requête CORS. Oublier les sous-domaines revient à refuser toute
 * écriture à toutes les écoles.
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
