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
 * Le portail parent/élève n'existe pas (ADR-010) : PARENT et STUDENT s'authentifient mais
 * restent interdits sur les APIs staff.
 */
class RoleAccessMatrixTest extends AbstractIntegrationTest {

    private record Endpoint(String method, String path, Set<Role> allowedTenantRoles) {
    }

    private static final Endpoint[] TENANT_ENDPOINTS = {
        new Endpoint("GET", "/api/v1/users/me", EnumSet.allOf(Role.class)),
        new Endpoint("GET", "/api/v1/dashboard/summary", EnumSet.of(Role.ADMIN, Role.DIRECTION)),
        new Endpoint("GET", "/api/v1/students", EnumSet.of(Role.ADMIN, Role.DIRECTION, Role.SECRETARY)),
        new Endpoint("GET", "/api/v1/teachers", EnumSet.of(Role.ADMIN, Role.DIRECTION)),
        new Endpoint("GET", "/api/v1/exams", EnumSet.of(Role.ADMIN, Role.DIRECTION, Role.TEACHER)),
        new Endpoint("GET", "/api/v1/attendance", EnumSet.of(Role.ADMIN, Role.DIRECTION, Role.TEACHER, Role.VIE_SCOLAIRE)),
        new Endpoint("GET", "/api/v1/timetable-entries", EnumSet.of(Role.ADMIN, Role.DIRECTION, Role.TEACHER)),
        new Endpoint("POST", "/api/v1/timetable-entries", EnumSet.of(Role.ADMIN, Role.DIRECTION)),
        new Endpoint("GET", "/api/v1/admin/dashboard/summary", EnumSet.noneOf(Role.class))
    };

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
                        .content(
                                "{\"schoolClassId\":1,\"subjectId\":1,\"teacherId\":1,\"roomId\":1,\"dayOfWeek\":\"MONDAY\",\"startTime\":\"08:00\",\"endTime\":\"09:00\"}")
                : get(endpoint.path());
        return request.header("Authorization", "Bearer " + token);
    }
}
