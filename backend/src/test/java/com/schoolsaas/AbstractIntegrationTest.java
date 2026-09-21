package com.schoolsaas;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base commune pour les tests d'intégration ayant besoin d'un vrai PostgreSQL (migrations
 * Flyway, Row-Level Security, filtre Hibernate).
 *
 * <p>Pattern "Singleton Container" de Testcontainers : le conteneur est démarré une seule
 * fois, manuellement, dans un bloc d'initialisation statique — <b>sans</b> l'annotation
 * {@code @Container} ni {@code @Testcontainers} (qui, même sur un champ statique hérité,
 * fait arrêter le conteneur par l'extension JUnit5 après CHAQUE classe de test concrète,
 * provoquant des erreurs "Connection refused" aléatoires dès qu'on a plus d'une classe de
 * test dans la suite). {@code @ServiceConnection} seul suffit à câbler la datasource ; le
 * nettoyage final est laissé au reaper Ryuk de Testcontainers à la fin de la JVM.
 *
 * <p>{@code application.yml} configure Flyway avec un rôle admin séparé (voir
 * docs/ARCHITECTURE.md ADR-001) dont les identifiants par défaut ne correspondent à rien en
 * test — {@code @DynamicPropertySource} les réaligne explicitement sur le conteneur (rôle
 * superutilisateur par défaut de Testcontainers, largement suffisant pour les migrations en
 * test).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
// La limitation de débit compte par adresse IP, or MockMvc présente toujours la même : active,
// elle ferait échouer en 429 des tests sans rapport avec elle. Déclarée ici en
// @TestPropertySource et non en @DynamicPropertySource, afin qu'un test ciblant le limiteur
// puisse la réactiver via @DynamicPropertySource, qui prime.
@TestPropertySource(properties = "app.rate-limit.enabled=false")
public abstract class AbstractIntegrationTest {

    @ServiceConnection
    protected static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void flywayConnectionProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.flyway.url", postgres::getJdbcUrl);
        registry.add("spring.flyway.user", postgres::getUsername);
        registry.add("spring.flyway.password", postgres::getPassword);
    }
}
