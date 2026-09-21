package com.schoolsaas.common.ratelimit;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolsaas.AbstractIntegrationTest;
import com.schoolsaas.auth.dto.TenantLoginRequest;
import com.schoolsaas.tenant.TenantResolver;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.GenericContainer;

/**
 * Vérifie le câblage, pas la mécanique du compteur (couverte par {@link RateLimiterTest}) :
 * la route de connexion consomme-t-elle réellement un quota ? Sans ce test, le limiteur
 * pourrait fonctionner parfaitement sans jamais être appelé.
 */
class RateLimitedLoginTest extends AbstractIntegrationTest {

    private static final GenericContainer<?> redis =
            new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

    static {
        redis.start();
    }

    @DynamicPropertySource
    static void rateLimitProperties(DynamicPropertyRegistry registry) {
        registry.add("app.rate-limit.enabled", () -> true);
        registry.add("app.rate-limit.login-per-ip", () -> 3);
        registry.add("app.rate-limit.login-per-account", () -> 100);
        registry.add(
                "spring.data.redis.url",
                () -> "redis://%s:%d".formatted(redis.getHost(), redis.getMappedPort(6379)));
    }

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void repeatedFailedLoginsAreEventuallyRefused() throws Exception {
        String body = objectMapper.writeValueAsString(
                new TenantLoginRequest("ecole-inexistante", "intrus@ecole.example", "mauvais-mot-de-passe"));

        // Le quota se consomme même sur des tentatives ratées : sinon une attaque par force
        // brute, qui échoue par définition jusqu'à réussir, ne coûterait jamais rien.
        for (int attempt = 0; attempt < 3; attempt++) {
            mockMvc.perform(post("/api/v1/auth/login")
                            .header(TenantResolver.TENANT_HEADER, 1L)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/v1/auth/login")
                        .header(TenantResolver.TENANT_HEADER, 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isTooManyRequests());
    }
}
