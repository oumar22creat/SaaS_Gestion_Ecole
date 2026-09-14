package com.schoolsaas.subject;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolsaas.AbstractIntegrationTest;
import com.schoolsaas.TestAuthSupport;
import com.schoolsaas.auth.Role;
import com.schoolsaas.auth.UserRepository;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/** CRUD matières (ROADMAP.md 1.5) + isolation cross-tenant. */
class SubjectTest extends AbstractIntegrationTest {

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
    private SubjectRepository subjectRepository;

    @Test
    void rejectsADuplicateSubjectCodeWithinTheSameTenant() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Matière");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "admin-subject@ecole.example", Role.ADMIN);

        mockMvc.perform(post("/api/v1/subjects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Français\",\"code\":\"FR\",\"coefficient\":2}"))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/v1/subjects")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Français bis\",\"code\":\"FR\",\"coefficient\":1}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("SUBJECT_CODE_ALREADY_USED"));
    }

    @Test
    void sameSubjectCodeIsAllowedAcrossDifferentTenants() throws Exception {
        Tenant tenantA = TestAuthSupport.createActiveTenant(tenantRepository, "École Matière A");
        Tenant tenantB = TestAuthSupport.createActiveTenant(tenantRepository, "École Matière B");
        subjectRepository.save(TestAuthSupport.withTenant(new Subject("Français", "FR", 2), tenantA.getId()));

        String tokenB = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenantB, "admin-b-subject@ecole.example", Role.ADMIN);

        mockMvc.perform(post("/api/v1/subjects")
                        .header("Authorization", "Bearer " + tokenB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Français\",\"code\":\"FR\",\"coefficient\":2}"))
                .andExpect(status().isCreated());
    }

    @Test
    void aTenantCanNeverSeeSubjectsOfAnotherTenant() throws Exception {
        Tenant tenantA = TestAuthSupport.createActiveTenant(tenantRepository, "École Matière C");
        Tenant tenantB = TestAuthSupport.createActiveTenant(tenantRepository, "École Matière D");
        Subject subjectB = subjectRepository.save(TestAuthSupport.withTenant(new Subject("Secret", "SEC", 1), tenantB.getId()));

        String tokenA = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenantA, "admin-a-subject@ecole.example", Role.ADMIN);

        mockMvc.perform(get("/api/v1/subjects/" + subjectB.getId()).header("Authorization", "Bearer " + tokenA))
                .andExpect(status().isNotFound());
    }
}
