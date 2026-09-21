package com.schoolsaas.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolsaas.AbstractIntegrationTest;
import com.schoolsaas.auth.dto.CreateUserRequest;
import com.schoolsaas.auth.dto.TenantLoginRequest;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantRepository;
import com.schoolsaas.tenant.TenantResolver;
import com.schoolsaas.tenant.TenantStatus;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Création des comptes du personnel (POST /api/v1/users). Jusqu'ici, seul le compte
 * Administrateur créé à l'inscription pouvait exister : aucun enseignant ne pouvait se
 * connecter, ce qui rendait l'application mobile inutilisable.
 */
class UserCreationTest extends AbstractIntegrationTest {

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

    private Tenant createTenant() {
        return tenantRepository.save(new Tenant("École Test", "ecole-" + UUID.randomUUID(), TenantStatus.ACTIVE));
    }

    private User createUser(Tenant tenant, String email, String rawPassword, Role role) {
        User user = new User(email, passwordEncoder.encode(rawPassword), "Awa", "Traoré", role);
        user.setSchoolId(tenant.getId());
        return userRepository.save(user);
    }

    private String loginAs(Tenant tenant, String email, String rawPassword) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .header(TenantResolver.TENANT_HEADER, tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TenantLoginRequest(tenant.getSubdomain(), email, rawPassword))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();
    }

    private MvcResult createAccount(Tenant tenant, String token, CreateUserRequest request, int expectedStatus)
            throws Exception {
        return mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .header(TenantResolver.TENANT_HEADER, tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is(expectedStatus))
                .andReturn();
    }

    @Test
    void administratorCreatesTeacherAccountThatCanLogIn() throws Exception {
        Tenant tenant = createTenant();
        createUser(tenant, "admin@ecole.example", "Sup3rSecret!", Role.ADMIN);
        String adminToken = loginAs(tenant, "admin@ecole.example", "Sup3rSecret!");

        createAccount(tenant, adminToken,
                new CreateUserRequest("prof@ecole.example", "Enseign4nt!", "Modibo", "Keita", Role.TEACHER),
                201);

        // Le compte créé doit réellement pouvoir se connecter : c'est tout l'intérêt.
        String teacherToken = loginAs(tenant, "prof@ecole.example", "Enseign4nt!");
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + teacherToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("TEACHER"))
                .andExpect(jsonPath("$.data.email").value("prof@ecole.example"));
    }

    @Test
    void rejectsDuplicateEmailWithinSameSchool() throws Exception {
        Tenant tenant = createTenant();
        createUser(tenant, "admin2@ecole.example", "Sup3rSecret!", Role.ADMIN);
        String adminToken = loginAs(tenant, "admin2@ecole.example", "Sup3rSecret!");

        CreateUserRequest request =
                new CreateUserRequest("doublon@ecole.example", "Secret123!", "Fanta", "Diarra", Role.SECRETARY);
        createAccount(tenant, adminToken, request, 201);
        createAccount(tenant, adminToken, request, 409);
    }

    @Test
    void rejectsRolesWithoutApplicationAccess() throws Exception {
        Tenant tenant = createTenant();
        createUser(tenant, "admin3@ecole.example", "Sup3rSecret!", Role.ADMIN);
        String adminToken = loginAs(tenant, "admin3@ecole.example", "Sup3rSecret!");

        // Aucun portail élève/parent n'existe (ADR-010) : ces comptes n'auraient aucun écran.
        createAccount(tenant, adminToken,
                new CreateUserRequest("eleve@ecole.example", "Secret123!", "Oumar", "Sidibé", Role.STUDENT),
                422);
    }

    @Test
    void teacherCannotCreateAccounts() throws Exception {
        Tenant tenant = createTenant();
        createUser(tenant, "prof-seul@ecole.example", "Sup3rSecret!", Role.TEACHER);
        String teacherToken = loginAs(tenant, "prof-seul@ecole.example", "Sup3rSecret!");

        createAccount(tenant, teacherToken,
                new CreateUserRequest("intrus@ecole.example", "Secret123!", "Sékou", "Coulibaly", Role.ADMIN),
                403);
    }

    /**
     * Isolation multi-tenant (CLAUDE.md) : l'Administrateur de l'école A crée un compte, et ce
     * compte ne doit exister que chez A. L'école B ne doit ni le voir, ni pouvoir s'y connecter.
     */
    @Test
    void createdAccountStaysInsideItsOwnSchool() throws Exception {
        Tenant schoolA = createTenant();
        Tenant schoolB = createTenant();
        createUser(schoolA, "admin-a@ecole.example", "Sup3rSecret!", Role.ADMIN);
        createUser(schoolB, "admin-b@ecole.example", "Sup3rSecret!", Role.ADMIN);

        String tokenA = loginAs(schoolA, "admin-a@ecole.example", "Sup3rSecret!");
        createAccount(schoolA, tokenA,
                new CreateUserRequest("compta@ecole.example", "Compt4ble!", "Aïcha", "Touré", Role.ACCOUNTANT),
                201);

        // L'école B ne voit pas le compte dans sa liste.
        String tokenB = loginAs(schoolB, "admin-b@ecole.example", "Sup3rSecret!");
        MvcResult listB = mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + tokenB)
                        .header(TenantResolver.TENANT_HEADER, schoolB.getId()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode dataB = objectMapper.readTree(listB.getResponse().getContentAsString()).get("data");
        assertThat(dataB.toString()).doesNotContain("compta@ecole.example");

        // Et le compte ne peut pas servir à se connecter chez B.
        mockMvc.perform(post("/api/v1/auth/login")
                        .header(TenantResolver.TENANT_HEADER, schoolB.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TenantLoginRequest(schoolB.getSubdomain(), "compta@ecole.example", "Compt4ble!"))))
                .andExpect(status().isUnauthorized());
    }

    private MvcResult put(Tenant tenant, String token, String path, Object body, int expectedStatus)
            throws Exception {
        return mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .put(path)
                        .header("Authorization", "Bearer " + token)
                        .header(TenantResolver.TENANT_HEADER, tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().is(expectedStatus))
                .andReturn();
    }

    private Long idOf(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("id").asLong();
    }

    @Test
    void deactivatedAccountCanNoLongerLogIn() throws Exception {
        Tenant tenant = createTenant();
        createUser(tenant, "admin-d@ecole.example", "Sup3rSecret!", Role.ADMIN);
        String adminToken = loginAs(tenant, "admin-d@ecole.example", "Sup3rSecret!");

        Long teacherId = idOf(createAccount(tenant, adminToken,
                new CreateUserRequest("adesactiver@ecole.example", "Enseign4nt!", "Modibo", "Keita", Role.TEACHER),
                201));
        loginAs(tenant, "adesactiver@ecole.example", "Enseign4nt!");

        put(tenant, adminToken, "/api/v1/users/" + teacherId + "/status",
                new com.schoolsaas.auth.dto.UpdateUserRequest.Status(false), 200);

        mockMvc.perform(post("/api/v1/auth/login")
                        .header(TenantResolver.TENANT_HEADER, tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TenantLoginRequest(
                                tenant.getSubdomain(), "adesactiver@ecole.example", "Enseign4nt!"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void resetPasswordReplacesTheOldOne() throws Exception {
        Tenant tenant = createTenant();
        createUser(tenant, "admin-p@ecole.example", "Sup3rSecret!", Role.ADMIN);
        String adminToken = loginAs(tenant, "admin-p@ecole.example", "Sup3rSecret!");

        Long id = idOf(createAccount(tenant, adminToken,
                new CreateUserRequest("oubli@ecole.example", "Ancien123!", "Fanta", "Diarra", Role.SECRETARY),
                201));

        put(tenant, adminToken, "/api/v1/users/" + id + "/password",
                new com.schoolsaas.auth.dto.UpdateUserRequest.PasswordReset("Nouveau456!"), 200);

        loginAs(tenant, "oubli@ecole.example", "Nouveau456!");
        mockMvc.perform(post("/api/v1/auth/login")
                        .header(TenantResolver.TENANT_HEADER, tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TenantLoginRequest(
                                tenant.getSubdomain(), "oubli@ecole.example", "Ancien123!"))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void roleChangeTakesEffectOnNextLogin() throws Exception {
        Tenant tenant = createTenant();
        createUser(tenant, "admin-r@ecole.example", "Sup3rSecret!", Role.ADMIN);
        String adminToken = loginAs(tenant, "admin-r@ecole.example", "Sup3rSecret!");

        Long id = idOf(createAccount(tenant, adminToken,
                new CreateUserRequest("promu@ecole.example", "Secret123!", "Aïcha", "Touré", Role.SECRETARY),
                201));

        put(tenant, adminToken, "/api/v1/users/" + id + "/role",
                new com.schoolsaas.auth.dto.UpdateUserRequest.RoleChange(Role.ACCOUNTANT), 200);

        String token = loginAs(tenant, "promu@ecole.example", "Secret123!");
        mockMvc.perform(get("/api/v1/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ACCOUNTANT"));
    }

    /** Sans ce garde-fou, un administrateur peut se couper l'accès sans aucun recours. */
    @Test
    void administratorCannotLockThemselvesOut() throws Exception {
        Tenant tenant = createTenant();
        User admin = createUser(tenant, "admin-s@ecole.example", "Sup3rSecret!", Role.ADMIN);
        String adminToken = loginAs(tenant, "admin-s@ecole.example", "Sup3rSecret!");

        put(tenant, adminToken, "/api/v1/users/" + admin.getId() + "/status",
                new com.schoolsaas.auth.dto.UpdateUserRequest.Status(false), 422);
        put(tenant, adminToken, "/api/v1/users/" + admin.getId() + "/role",
                new com.schoolsaas.auth.dto.UpdateUserRequest.RoleChange(Role.TEACHER), 422);
    }
}
