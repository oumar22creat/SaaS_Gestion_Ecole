package com.schoolsaas.auth;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * Refuse le démarrage hors développement si la clé de signature des jetons est restée celle
 * du dépôt.
 *
 * <p>La valeur de repli de {@code application.yml} rend l'application utilisable après un
 * simple clone, ce qui est souhaitable en local. Mais elle est publique : quiconque lit le
 * dépôt peut forger un jeton d'administrateur de n'importe quel établissement. Sans ce
 * garde-fou, un déploiement où {@code JWT_SECRET} a été oublié démarre normalement et
 * n'émet aucun signal — la faille reste invisible jusqu'à son exploitation.
 *
 * <p>Échouer au démarrage est ici le comportement sûr : une application qui ne démarre pas
 * se remarque tout de suite, une application ouverte à tous non.
 */
@Component
public class JwtSecretGuard {

    /** Doit rester identique à la valeur de repli de `app.jwt.secret` dans application.yml. */
    static final String DEVELOPMENT_SECRET = "dev-only-insecure-secret-do-not-use-in-production-min-32-bytes";

    /** HS384 (voir JwtService) : en deçà, la clé affaiblit l'algorithme. */
    private static final int MINIMUM_SECRET_BYTES = 32;

    private static final List<String> DEVELOPMENT_PROFILES = List.of("dev", "test");

    private final JwtProperties jwtProperties;
    private final Environment environment;

    public JwtSecretGuard(JwtProperties jwtProperties, Environment environment) {
        this.jwtProperties = jwtProperties;
        this.environment = environment;
    }

    @PostConstruct
    void verifySecret() {
        if (isDevelopmentEnvironment()) {
            return;
        }

        String secret = jwtProperties.secret();
        if (secret == null || secret.isBlank() || DEVELOPMENT_SECRET.equals(secret)) {
            throw new IllegalStateException(
                    "JWT_SECRET n'est pas configuré : l'application refuse de démarrer avec la clé de "
                            + "développement, publique dans le dépôt. Générer une valeur aléatoire d'au moins "
                            + MINIMUM_SECRET_BYTES + " octets et la fournir via la variable d'environnement.");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < MINIMUM_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET est trop court : " + MINIMUM_SECRET_BYTES + " octets minimum sont requis "
                            + "pour la signature HS384.");
        }
    }

    private boolean isDevelopmentEnvironment() {
        String[] active = environment.getActiveProfiles();
        if (active.length == 0) {
            // Aucun profil explicite : on ne peut pas affirmer qu'il s'agit de développement,
            // donc on applique la règle stricte plutôt que de supposer le cas favorable.
            return false;
        }
        return List.of(active).stream().anyMatch(DEVELOPMENT_PROFILES::contains);
    }
}
