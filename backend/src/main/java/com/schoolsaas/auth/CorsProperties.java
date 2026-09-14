package com.schoolsaas.auth;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Origines autorisées à appeler l'API depuis un navigateur (voir docs/ARCHITECTURE.md).
 * Nécessaire uniquement en dev/staging où le Web (Angular, `ng serve`) tourne sur une origine
 * différente du backend — en production, Nginx sert les deux sur le même domaine (voir
 * README.md "Environnements"), donc aucune requête CORS n'y transite réellement.
 */
@ConfigurationProperties(prefix = "app.cors")
public record CorsProperties(List<String> allowedOrigins) {
}
