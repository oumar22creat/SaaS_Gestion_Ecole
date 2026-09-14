package com.schoolsaas.tenant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolsaas.AbstractIntegrationTest;
import com.schoolsaas.tenant.dto.TenantRegistrationRequest;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.JdbcConnectionDetails;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Reproduit l'inscription self-service avec le rôle applicatif RESTREINT (pas le
 * superutilisateur par défaut de Testcontainers utilisé par {@code TenantRegistrationTest})
 * — exactement le rôle utilisé en pratique via docker-compose (voir
 * {@code backend/docker/postgres-init/01-create-app-role.sh}).
 *
 * <p>Un superutilisateur PostgreSQL contourne TOUJOURS Row-Level Security (ADR-001, Piège
 * 4) : les tests d'inscription existants passaient donc même avec un bug d'ordre entre
 * l'activation du contexte tenant et l'INSERT dans {@code users}, puisque RLS n'était de
 * fait jamais évalué. Ce test a mis en évidence, en conditions réelles (voir aussi le
 * scénario Playwright manuel qui a révélé le problème), que la politique RLS sur
 * {@code users} (pas de clause WITH CHECK explicite → Postgres réutilise USING, qui
 * s'appuie sur {@code current_setting('app.tenant_id', true)}) rejetait l'INSERT de
 * l'Administrateur initial tant que ce réglage de session n'était pas posé AVANT cet INSERT
 * — corrigé dans {@link TenantRegistrationService#register}.
 *
 * <p><b>Piège de test rencontré</b> : {@code @ServiceConnection} (sur le champ {@code
 * postgres} de {@code AbstractIntegrationTest}) enregistre un bean {@code
 * JdbcConnectionDetails} qui a la PRIORITÉ sur un simple {@code @DynamicPropertySource}
 * positionnant {@code spring.datasource.username/password} — la première version de ce test
 * continuait silencieusement à se connecter avec le rôle superutilisateur et passait donc à
 * tort même sans le correctif. Fixé en fournissant notre propre bean {@code
 * JdbcConnectionDetails} (voir {@code RestrictedRoleConnectionDetails}), qui prend le pas sur
 * celui dérivé de {@code @ServiceConnection} via {@code @ConditionalOnMissingBean}.
 */
@Import(TenantRegistrationRlsTest.RestrictedRoleConnectionDetails.class)
class TenantRegistrationRlsTest extends AbstractIntegrationTest {

    private static final String APP_ROLE = "reg_rls_test_role";

    static {
        try (Connection connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                Statement statement = connection.createStatement()) {
            statement.execute(
                    "DO $$ BEGIN IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '" + APP_ROLE + "') THEN "
                            + "CREATE ROLE " + APP_ROLE + " LOGIN PASSWORD '" + APP_ROLE + "' NOSUPERUSER; END IF; END $$;");
            statement.execute("GRANT CONNECT ON DATABASE " + postgres.getDatabaseName() + " TO " + APP_ROLE);
            statement.execute("GRANT USAGE ON SCHEMA public TO " + APP_ROLE);
            // Le conteneur Postgres est un singleton partagé par TOUTE la suite (voir
            // AbstractIntegrationTest) : selon l'ordre d'exécution des classes de test,
            // Flyway a pu déjà créer les tables via une AUTRE classe avant que ce bloc static
            // ne s'exécute — ALTER DEFAULT PRIVILEGES seul (qui ne couvre que les tables
            // FUTURES) ne suffit donc pas ; il faut aussi accorder explicitement sur les
            // tables déjà existantes à cet instant.
            statement.execute("GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO " + APP_ROLE);
            statement.execute("GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO " + APP_ROLE);
            statement.execute(
                    "ALTER DEFAULT PRIVILEGES FOR ROLE " + postgres.getUsername() + " IN SCHEMA public "
                            + "GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO " + APP_ROLE);
            statement.execute(
                    "ALTER DEFAULT PRIVILEGES FOR ROLE " + postgres.getUsername() + " IN SCHEMA public "
                            + "GRANT USAGE, SELECT ON SEQUENCES TO " + APP_ROLE);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * spring.flyway.* reste sur le rôle admin (superutilisateur) posé par la classe parente
     * — seul le rôle RUNTIME de l'application (celui utilisé par le pool Hikari applicatif)
     * est restreint ici, comme en production (voir application.yml : spring.datasource.*
     * vs spring.flyway.*).
     */
    @TestConfiguration
    static class RestrictedRoleConnectionDetails {

        @Bean
        @Primary
        JdbcConnectionDetails jdbcConnectionDetails() {
            return new JdbcConnectionDetails() {
                @Override
                public String getUsername() {
                    return APP_ROLE;
                }

                @Override
                public String getPassword() {
                    return APP_ROLE;
                }

                @Override
                public String getJdbcUrl() {
                    return postgres.getJdbcUrl();
                }
            };
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registrationSucceedsUnderRowLevelSecurityWithARestrictedRuntimeRole() throws Exception {
        String subdomain = "ecole-rls-" + UUID.randomUUID().toString().substring(0, 8);
        var request = new TenantRegistrationRequest(
                "École RLS", subdomain, "admin@" + subdomain + ".example", "Sup3rSecret!", "Ada", "Lovelace");

        mockMvc.perform(post("/api/v1/tenants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.tokens.accessToken").isNotEmpty());
    }
}
