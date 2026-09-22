package com.schoolsaas.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolsaas.AbstractIntegrationTest;
import com.schoolsaas.TestAuthSupport;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantRepository;
import java.util.EnumSet;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

/**
 * Matrice RBAC des 8 rôles établissement + Super-Admin (cahier-des-charges.md §5, ADR-008).
 * PARENT et STUDENT ont leur propre portail (/api/v1/portal/**, voir PortalAccessTest) et
 * restent interdits sur toutes les APIs du personnel listées ici.
 *
 * <p>Plusieurs ressources distinguent lecture et écriture : la liste des classes, des élèves,
 * des matières et des salles est le point d'entrée des écrans de tout le personnel (feuille
 * d'appel, notes, frais, cantine, discipline), alors que les créer ou les modifier reste à la
 * direction. Les deux sens sont épinglés ci-dessous : élargir une lecture ne doit jamais
 * élargir l'écriture au passage.
 */
class RoleAccessMatrixTest extends AbstractIntegrationTest {

    private record Endpoint(String method, String path, Set<Role> allowedTenantRoles) {
    }

    /** Tout le personnel de l'établissement — élèves et parents exclus. */
    private static final Set<Role> STAFF = EnumSet.of(
            Role.ADMIN, Role.DIRECTION, Role.TEACHER, Role.SECRETARY, Role.VIE_SCOLAIRE, Role.ACCOUNTANT);

    private static final Endpoint[] TENANT_ENDPOINTS = {
        new Endpoint("GET", "/api/v1/users/me", EnumSet.allOf(Role.class)),
        new Endpoint("GET", "/api/v1/dashboard/summary", EnumSet.of(Role.ADMIN, Role.DIRECTION)),
        new Endpoint("GET", "/api/v1/students", STAFF),
        new Endpoint("POST", "/api/v1/students", EnumSet.of(Role.ADMIN, Role.DIRECTION, Role.SECRETARY)),
        new Endpoint("GET", "/api/v1/classes", STAFF),
        new Endpoint("POST", "/api/v1/classes", EnumSet.of(Role.ADMIN, Role.DIRECTION)),
        new Endpoint("GET", "/api/v1/subjects", EnumSet.of(Role.ADMIN, Role.DIRECTION, Role.TEACHER)),
        new Endpoint("POST", "/api/v1/subjects", EnumSet.of(Role.ADMIN, Role.DIRECTION)),
        new Endpoint("GET", "/api/v1/rooms", EnumSet.of(Role.ADMIN, Role.DIRECTION, Role.TEACHER)),
        new Endpoint("POST", "/api/v1/rooms", EnumSet.of(Role.ADMIN, Role.DIRECTION)),
        new Endpoint("GET", "/api/v1/teachers", EnumSet.of(Role.ADMIN, Role.DIRECTION)),
        new Endpoint("GET", "/api/v1/exams", EnumSet.of(Role.ADMIN, Role.DIRECTION, Role.TEACHER)),
        new Endpoint("GET", "/api/v1/attendance", EnumSet.of(Role.ADMIN, Role.DIRECTION, Role.TEACHER, Role.VIE_SCOLAIRE)),
        new Endpoint("GET", "/api/v1/timetable-entries", EnumSet.of(Role.ADMIN, Role.DIRECTION, Role.TEACHER)),
        new Endpoint("POST", "/api/v1/timetable-entries", EnumSet.of(Role.ADMIN, Role.DIRECTION)),
        new Endpoint("GET", "/api/v1/admin/dashboard/summary", EnumSet.noneOf(Role.class))
    };

    /**
     * Corps minimal accepté par chaque POST testé. Le contenu importe peu — la matrice porte
     * sur le 403, pas sur la validation — mais un corps absent ferait échouer la requête avant
     * même le contrôle de rôle, et le test ne prouverait plus rien.
     */
    private static String bodyFor(String path) {
        return switch (path) {
            case "/api/v1/students" ->
                "{\"studentNumber\":\"RBAC-1\",\"firstName\":\"A\",\"lastName\":\"B\","
                        + "\"birthDate\":\"2012-01-01\",\"gender\":\"M\"}";
            case "/api/v1/classes" -> "{\"name\":\"RBAC\",\"level\":\"6eme\",\"capacity\":30}";
            case "/api/v1/subjects" -> "{\"name\":\"RBAC\",\"code\":\"RBAC\",\"coefficient\":1}";
            case "/api/v1/rooms" -> "{\"name\":\"RBAC\",\"capacity\":30}";
            default ->
                "{\"schoolClassId\":1,\"subjectId\":1,\"teacherId\":1,\"roomId\":1,"
                        + "\"dayOfWeek\":\"MONDAY\",\"startTime\":\"08:00\",\"endTime\":\"09:00\"}";
        };
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private PlatformAdminRepository platformAdminRepository;

    @Test
    void eachTenantRoleIsAllowedOrDeniedAccordingToTheMatrix() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Matrice RBAC");

        for (Role role : Role.values()) {
            String email = role.name().toLowerCase() + "-" + tenant.getId() + "@ecole.example";
            String token = TestAuthSupport.createUserAndLogin(
                    mockMvc, objectMapper, userRepository, passwordEncoder, tenant, email, role);

            for (Endpoint endpoint : TENANT_ENDPOINTS) {
                int status = mockMvc.perform(authorized(endpoint, token)).andReturn().getResponse().getStatus();
                if (endpoint.allowedTenantRoles().contains(role)) {
                    assertThat(status)
                            .as("%s %s %s", role, endpoint.method(), endpoint.path())
                            .isNotEqualTo(403);
                } else {
                    assertThat(status)
                            .as("%s %s %s", role, endpoint.method(), endpoint.path())
                            .isEqualTo(403);
                }
            }
        }
    }

    @Test
    void superAdminCanReadThePlatformDashboardButNotTenantStaffApis() throws Exception {
        String token = TestAuthSupport.createPlatformAdminAndLogin(
                mockMvc,
                objectMapper,
                platformAdminRepository,
                passwordEncoder,
                "super-matrix@platform.example");

        mockMvc.perform(get("/api/v1/admin/dashboard/summary").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/students").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/dashboard/summary").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/exams").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    private MockHttpServletRequestBuilder authorized(Endpoint endpoint, String token) {
        MockHttpServletRequestBuilder request = "POST".equals(endpoint.method())
                ? post(endpoint.path())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(bodyFor(endpoint.path()))
                : get(endpoint.path());
        return request.header("Authorization", "Bearer " + token);
    }
}
