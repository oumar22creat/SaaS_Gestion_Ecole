package com.schoolsaas.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolsaas.AbstractIntegrationTest;
import com.schoolsaas.auth.dto.RefreshRequest;
import com.schoolsaas.auth.dto.TenantLoginRequest;
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
import org.springframework.test.web.servlet.MvcResult;

/**
 * Reproduit login/refresh avec le rôle applicatif RESTREINT (comme
 * {@link com.schoolsaas.tenant.TenantRegistrationRlsTest}, dont ce test reprend le
 * mécanisme — dupliqué plutôt que partagé, précédent déjà établi par
 * {@code TenantIsolationTest}/{@code TenantRegistrationRlsTest}).
 *
 * <p>Sous le superutilisateur par défaut de Testcontainers (RLS toujours contourné, voir
 * ADR-001 Piège 4), ce bug était invisible : {@code AuthService.login}/{@code resolvePrincipal}
 * faisaient une recherche dans {@code users} (table sous {@code FORCE ROW LEVEL SECURITY})
 * sans avoir positionné {@code app.tenant_id} au préalable. En conditions réelles (rôle
 * applicatif restreint + connexions poolées réutilisées entre requêtes), le résultat dépendait
 * de la valeur laissée par une requête précédente sur la même connexion — voir
 * docs/ARCHITECTURE.md ADR-029.
 */
@Import(AuthRlsTest.RestrictedRoleConnectionDetails.class)
class AuthRlsTest extends AbstractIntegrationTest {

    private static final String APP_ROLE = "auth_rls_test_role";

    static {
        try (Connection connection = DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                Statement statement = connection.createStatement()) {
            statement.execute(
                    "DO $$ BEGIN IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = '" + APP_ROLE + "') THEN "
                            + "CREATE ROLE " + APP_ROLE + " LOGIN PASSWORD '" + APP_ROLE + "' NOSUPERUSER; END IF; END $$;");
            statement.execute("GRANT CONNECT ON DATABASE " + postgres.getDatabaseName() + " TO " + APP_ROLE);
            statement.execute("GRANT USAGE ON SCHEMA public TO " + APP_ROLE);
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
    void loginThenRefreshSucceedUnderRowLevelSecurityWithARestrictedRuntimeRole() throws Exception {
        String subdomain = "ecole-auth-rls-" + UUID.randomUUID().toString().substring(0, 8);
        String email = "admin@" + subdomain + ".example";
        var registration = new TenantRegistrationRequest(subdomain, subdomain, email, "Sup3rSecret!", "Ada", "Lovelace");

        mockMvc.perform(post("/api/v1/tenants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registration)))
                .andExpect(status().isCreated());

        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TenantLoginRequest(subdomain, email, "Sup3rSecret!"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andReturn();

        JsonNode loginData = objectMapper.readTree(loginResult.getResponse().getContentAsString()).get("data");
        String refreshToken = loginData.get("refreshToken").asText();

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshRequest(refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());
    }
}
