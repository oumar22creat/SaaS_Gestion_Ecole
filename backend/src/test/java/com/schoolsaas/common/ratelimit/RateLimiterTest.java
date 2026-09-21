package com.schoolsaas.common.ratelimit;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import com.schoolsaas.common.ApiException;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.testcontainers.containers.GenericContainer;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;

/**
 * Deux comportements distincts sont vérifiés ici : que la limite arrête réellement les
 * tentatives (contre un vrai Redis), et qu'une panne du limiteur laisse passer plutôt que de
 * bloquer l'application entière.
 */
class RateLimiterTest {

    private static final GenericContainer<?> redis =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    static {
        redis.start();
    }

    private StringRedisTemplate template;

    @BeforeEach
    void setUp() {
        LettuceConnectionFactory factory = new LettuceConnectionFactory(
                new RedisStandaloneConfiguration(redis.getHost(), redis.getMappedPort(6379)));
        factory.afterPropertiesSet();
        template = new StringRedisTemplate(factory);
        template.afterPropertiesSet();
    }

    private RateLimiter limiter(boolean enabled, int unusedLimit) {
        return new RateLimiter(
                template, new RateLimitProperties(enabled, unusedLimit, unusedLimit, unusedLimit, Duration.ofMinutes(15)));
    }

    @Test
    void blocksOnceTheThresholdIsPassed() {
        RateLimiter rateLimiter = limiter(true, 3);
        String identity = "192.0.2.10-" + System.nanoTime();

        for (int attempt = 0; attempt < 3; attempt++) {
            rateLimiter.consume("login-ip", identity, 3);
        }

        assertThatThrownBy(() -> rateLimiter.consume("login-ip", identity, 3))
                .isInstanceOf(ApiException.class)
                .hasMessageContaining("Trop de tentatives");
    }

    /** Deux usages différents ne doivent jamais partager le même compteur. */
    @Test
    void countersAreIsolatedByScope() {
        RateLimiter rateLimiter = limiter(true, 1);
        String identity = "192.0.2.11-" + System.nanoTime();

        rateLimiter.consume("login-ip", identity, 1);
        assertThatCode(() -> rateLimiter.consume("tenant-registration", identity, 1))
                .doesNotThrowAnyException();
    }

    /**
     * Arbitrage assumé : refuser les connexions parce que le limiteur est en panne
     * transformerait un incident secondaire en panne totale de l'application.
     */
    @Test
    void letsTrafficThroughWhenRedisIsUnreachable() {
        // Un vrai client pointé sur un port où rien n'écoute : on exerce le chemin d'échec
        // réel (timeout de connexion) plutôt qu'une exception simulée.
        LettuceConnectionFactory deadFactory =
                new LettuceConnectionFactory(new RedisStandaloneConfiguration("127.0.0.1", 6399));
        deadFactory.afterPropertiesSet();
        StringRedisTemplate unreachable = new StringRedisTemplate(deadFactory);
        unreachable.afterPropertiesSet();

        RateLimiter rateLimiter = new RateLimiter(
                unreachable, new RateLimitProperties(true, 1, 1, 1, Duration.ofMinutes(15)));

        assertThatCode(() -> rateLimiter.consume("login-ip", "192.0.2.12", 1)).doesNotThrowAnyException();
    }

    @Test
    void doesNothingWhenDisabled() {
        RateLimiter rateLimiter = limiter(false, 1);
        String identity = "192.0.2.13-" + System.nanoTime();

        assertThatCode(() -> {
            for (int attempt = 0; attempt < 5; attempt++) {
                rateLimiter.consume("login-ip", identity, 1);
            }
        }).doesNotThrowAnyException();
    }
}
