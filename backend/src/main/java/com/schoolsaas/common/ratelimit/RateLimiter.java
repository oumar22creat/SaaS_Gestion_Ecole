package com.schoolsaas.common.ratelimit;

import com.schoolsaas.common.ApiException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HexFormat;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Limitation de débit par fenêtre fixe, adossée à Redis.
 *
 * <p>Redis et non un compteur en mémoire : en production l'API tourne derrière plusieurs
 * instances, et un compteur local se contournerait en répartissant les tentatives.
 *
 * <p><b>En cas d'indisponibilité de Redis, la limitation laisse passer.</b> C'est un arbitrage
 * assumé : refuser les connexions parce que le limiteur est en panne transformerait un
 * incident d'infrastructure secondaire en panne totale de l'application. L'incident est
 * journalisé en erreur pour qu'il ne passe pas inaperçu.
 */
@Component
public class RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);

    private final StringRedisTemplate redisTemplate;
    private final RateLimitProperties properties;

    public RateLimiter(StringRedisTemplate redisTemplate, RateLimitProperties properties) {
        this.redisTemplate = redisTemplate;
        this.properties = properties;
    }

    /**
     * Consomme une unité du quota et lève une erreur si le seuil est dépassé.
     *
     * @param scope famille de quota (ex. {@code login-ip}), pour que deux usages ne partagent
     *              jamais le même compteur
     * @param identity valeur identifiant l'appelant ; elle est hachée avant de servir de clé,
     *                 afin qu'aucune adresse e-mail ne se retrouve en clair dans Redis
     */
    public void consume(String scope, String identity, int limit) {
        if (!properties.enabled() || limit <= 0) {
            return;
        }

        String key = "rl:%s:%s".formatted(scope, fingerprint(identity));
        Long hits;
        try {
            hits = redisTemplate.opsForValue().increment(key);
            if (hits != null && hits == 1L) {
                redisTemplate.expire(key, properties.window());
            }
        } catch (RuntimeException e) {
            log.error("Limitation de débit inopérante, Redis injoignable — requête laissée passer", e);
            return;
        }

        if (hits != null && hits > limit) {
            long minutes = Math.max(1, properties.window().toMinutes());
            throw ApiException.tooManyRequests(
                    "RATE_LIMIT_EXCEEDED",
                    "Trop de tentatives. Réessayez dans %d %s.".formatted(
                            minutes, minutes > 1 ? "minutes" : "minute"));
        }
    }

    /** Empêche qu'une adresse e-mail ou une IP soit lisible dans Redis. */
    private String fingerprint(String identity) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(String.valueOf(identity).toLowerCase().getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest, 0, 16);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponible", e);
        }
    }
}
