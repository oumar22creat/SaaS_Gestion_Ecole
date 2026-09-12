package com.schoolsaas.tenant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolsaas.AbstractIntegrationTest;
import com.schoolsaas.auth.Role;
import com.schoolsaas.auth.User;
import com.schoolsaas.auth.UserRepository;
import com.schoolsaas.auth.dto.LoginRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

/**
 * Test d'isolation multi-tenant exigé par CLAUDE.md règle 2 : "Tout endpoint retournant des
 * données scoped-tenant doit avoir un test vérifiant qu'un utilisateur d'un autre tenant ne
 * peut pas y accéder." Vérifie à la fois le mécanisme bas niveau (filtre Hibernate) et le
 * comportement observable à travers une vraie requête HTTP authentifiée.
 */
class TenantIsolationTest extends AbstractIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TenantSessionConfigurer tenantSessionConfigurer;

    @PersistenceContext
    private EntityManager entityManager;

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    private Tenant createTenant(String label) {
        return tenantRepository.save(new Tenant(label, "tenant-" + UUID.randomUUID(), TenantStatus.ACTIVE));
    }

    private User createUser(Tenant tenant, String email, Role role) {
        User user = new User(email, passwordEncoder.encode("Sup3rSecret!"), "Ada", "Lovelace", role);
        user.setSchoolId(tenant.getId());
        return userRepository.save(user);
    }

    @Test
    @Transactional
    void hibernateFilterHidesRowsFromOtherTenants() {
        Tenant tenantA = createTenant("École A");
        Tenant tenantB = createTenant("École B");
        createUser(tenantA, "admin-a@ecole.example", Role.ADMIN);
        User userB = createUser(tenantB, "prof-b@ecole.example", Role.TEACHER);

        // Vide le cache de premier niveau : sans ça, userB — juste inséré dans CETTE même
        // session — resterait trouvable par identité, filtre ou pas (find() par clé
        // primaire consulte d'abord le cache de session). Dans une vraie requête, userB
        // aurait été inséré dans une session complètement différente : ce clear() reproduit
        // fidèlement cette situation.
        entityManager.flush();
        entityManager.clear();

        TenantContext.set(tenantA.getId());
        // Reproduit exactement ce que fait TenantContextInterceptor en production.
        tenantSessionConfigurer.applyTenant(tenantA.getId());
        try {
            assertThat(userRepository.findById(userB.getId())).isEmpty();
            assertThat(userRepository.findAll())
                    .extracting(User::getEmail)
                    .doesNotContain("prof-b@ecole.example");
        } finally {
            TenantContext.clear();
        }
    }

    @Test
    void rowLevelSecurityAloneBlocksAccessForANonSuperuserRole() throws java.sql.SQLException {
        // La politique RLS (voir V2__create_users_table.sql) ne protège RIEN si le rôle qui se
        // connecte est superutilisateur PostgreSQL — RLS est TOUJOURS contourné pour un
        // superutilisateur, sans exception possible, y compris avec FORCE ROW LEVEL SECURITY
        // (voir la doc PostgreSQL sur ROW LEVEL SECURITY). Le rôle par défaut de
        // Testcontainers (comme celui créé par POSTGRES_USER dans docker-compose.yml) EST
        // superutilisateur : ce test crée donc son propre rôle restreint pour vérifier que la
        // politique elle-même fonctionne réellement, indépendamment de qui s'y connecte en
        // pratique — voir docs/ARCHITECTURE.md ADR-001 pour la conséquence sur le déploiement
        // réel (l'application doit se connecter avec un rôle applicatif non superutilisateur,
        // jamais avec le rôle utilisé par Flyway pour les migrations).
        //
        // Tout ce bloc passe par du JDBC brut (pas par l'EntityManager/repositories) : les
        // instructions DDL (CREATE ROLE, GRANT) doivent être commitées immédiatement pour être
        // visibles depuis la connexion séparée ouverte plus bas en tant que rôle restreint —
        // ce que ne garantirait pas une transaction de test Spring (rollback par défaut).
        Long tenantAId;
        Long userBId;
        try (java.sql.Connection setup = java.sql.DriverManager.getConnection(
                postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())) {
            try (java.sql.Statement statement = setup.createStatement()) {
                statement.execute(
                        "DO $$ BEGIN "
                                + "IF NOT EXISTS (SELECT FROM pg_roles WHERE rolname = 'rls_test_role') THEN "
                                + "CREATE ROLE rls_test_role LOGIN PASSWORD 'rls_test_role' NOSUPERUSER; "
                                + "END IF; END $$;");
                statement.execute("GRANT SELECT ON users TO rls_test_role");
                try (java.sql.ResultSet rs = statement.executeQuery(
                        "INSERT INTO tenants (name, subdomain, status) "
                                + "VALUES ('École A RLS', 'ecole-a-rls-" + UUID.randomUUID() + "', 'ACTIVE') "
                                + "RETURNING id")) {
                    rs.next();
                    tenantAId = rs.getLong(1);
                }
                Long tenantBId;
                try (java.sql.ResultSet rs = statement.executeQuery(
                        "INSERT INTO tenants (name, subdomain, status) "
                                + "VALUES ('École B RLS', 'ecole-b-rls-" + UUID.randomUUID() + "', 'ACTIVE') "
                                + "RETURNING id")) {
                    rs.next();
                    tenantBId = rs.getLong(1);
                }
                try (java.sql.ResultSet rs = statement.executeQuery(
                        "INSERT INTO users (school_id, email, password_hash, first_name, last_name, role) "
                                + "VALUES (" + tenantBId + ", 'prof-b-rls@ecole.example', 'x', 'Ada', 'Lovelace', 'TEACHER') "
                                + "RETURNING id")) {
                    rs.next();
                    userBId = rs.getLong(1);
                }
            }
        }

        try (java.sql.Connection connection = java.sql.DriverManager.getConnection(
                postgres.getJdbcUrl(), "rls_test_role", "rls_test_role")) {
            try (java.sql.Statement setTenant = connection.createStatement()) {
                setTenant.execute("SELECT set_config('app.tenant_id', '" + tenantAId + "', false)");
            }
            try (java.sql.Statement query = connection.createStatement();
                    java.sql.ResultSet resultSet = query.executeQuery(
                            "SELECT id FROM users WHERE id = " + userBId)) {
                assertThat(resultSet.next()).isFalse();
            }
        }
    }

    @Test
    void adminOfTenantACannotFetchUserOfTenantBById() throws Exception {
        Tenant tenantA = createTenant("École A");
        Tenant tenantB = createTenant("École B");
        createUser(tenantA, "admin-a2@ecole.example", Role.ADMIN);
        User userB = createUser(tenantB, "prof-b2@ecole.example", Role.TEACHER);

        String accessToken = loginAndGetAccessToken(tenantA, "admin-a2@ecole.example");

        // L'id de userB est parfaitement valide en base — juste pas dans le tenant courant.
        // Doit être invisible (404), jamais renvoyé (voir cahier-des-charges.md §2.2).
        mockMvc.perform(get("/api/v1/users/" + userB.getId())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }

    @Test
    void listingUsersOnlyReturnsCurrentTenant() throws Exception {
        Tenant tenantA = createTenant("École A");
        Tenant tenantB = createTenant("École B");
        createUser(tenantA, "admin-a3@ecole.example", Role.ADMIN);
        createUser(tenantB, "prof-b3@ecole.example", Role.TEACHER);

        String accessToken = loginAndGetAccessToken(tenantA, "admin-a3@ecole.example");

        mockMvc.perform(get("/api/v1/users").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    String body = result.getResponse().getContentAsString();
                    assertThat(body).doesNotContain("prof-b3@ecole.example");
                    assertThat(body).contains("admin-a3@ecole.example");
                });
    }

    private String loginAndGetAccessToken(Tenant tenant, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .header(TenantResolver.TENANT_HEADER, tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "Sup3rSecret!"))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        return data.get("accessToken").asText();
    }
}
