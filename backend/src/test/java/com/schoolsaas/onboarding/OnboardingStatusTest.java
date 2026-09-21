package com.schoolsaas.onboarding;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolsaas.AbstractIntegrationTest;
import com.schoolsaas.auth.Role;
import com.schoolsaas.auth.User;
import com.schoolsaas.auth.UserRepository;
import com.schoolsaas.auth.dto.TenantLoginRequest;
import com.schoolsaas.schoolclass.SchoolClass;
import com.schoolsaas.schoolclass.SchoolClassRepository;
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
 * Configuration initiale. L'avancement est déduit des données réelles et non d'un drapeau
 * stocké : le test vérifie donc surtout qu'il suit les données dans les deux sens.
 */
class OnboardingStatusTest extends AbstractIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private SchoolClassRepository schoolClassRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private ObjectMapper objectMapper;

    private Tenant createTenant() {
        return tenantRepository.save(
                new Tenant("École Test", "ecole-" + UUID.randomUUID(), TenantStatus.ACTIVE));
    }

    private String loginAsAdmin(Tenant tenant, String email) throws Exception {
        User user = new User(email, passwordEncoder.encode("Secret123!"), "Awa", "Traoré", Role.ADMIN);
        user.setSchoolId(tenant.getId());
        userRepository.save(user);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .header(TenantResolver.TENANT_HEADER, tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new TenantLoginRequest(tenant.getSubdomain(), email, "Secret123!"))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();
    }

    private MvcResult fetchStatus(Tenant tenant, String token) throws Exception {
        return mockMvc.perform(get("/api/v1/onboarding/status")
                        .header("Authorization", "Bearer " + token)
                        .header(TenantResolver.TENANT_HEADER, tenant.getId()))
                .andExpect(status().isOk())
                .andReturn();
    }

    @Test
    void aFreshSchoolHasEverythingLeftToDo() throws Exception {
        Tenant tenant = createTenant();
        String token = loginAsAdmin(tenant, "admin-neuf@ecole.example");

        mockMvc.perform(get("/api/v1/onboarding/status")
                        .header("Authorization", "Bearer " + token)
                        .header(TenantResolver.TENANT_HEADER, tenant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.complete").value(false))
                .andExpect(jsonPath("$.data.steps.length()").value(5))
                .andExpect(jsonPath("$.data.steps[0].key").value("CLASSES"))
                .andExpect(jsonPath("$.data.steps[0].done").value(false))
                .andExpect(jsonPath("$.data.steps[0].count").value(0));
    }

    /**
     * Le point du choix "déduit, pas stocké" : créer une classe coche l'étape, la supprimer
     * la décoche. Un drapeau en base aurait laissé l'école croire sa configuration faite.
     */
    @Test
    void progressFollowsTheDataInBothDirections() throws Exception {
        Tenant tenant = createTenant();
        String token = loginAsAdmin(tenant, "admin-suivi@ecole.example");

        SchoolClass schoolClass = new SchoolClass("6e A", null);
        schoolClass.setSchoolId(tenant.getId());
        schoolClass = schoolClassRepository.save(schoolClass);

        mockMvc.perform(get("/api/v1/onboarding/status")
                        .header("Authorization", "Bearer " + token)
                        .header(TenantResolver.TENANT_HEADER, tenant.getId()))
                .andExpect(jsonPath("$.data.steps[0].done").value(true))
                .andExpect(jsonPath("$.data.steps[0].count").value(1));

        schoolClassRepository.delete(schoolClass);

        mockMvc.perform(get("/api/v1/onboarding/status")
                        .header("Authorization", "Bearer " + token)
                        .header(TenantResolver.TENANT_HEADER, tenant.getId()))
                .andExpect(jsonPath("$.data.steps[0].done").value(false));
    }

    @Test
    void aTeacherCannotSeeTheSetupGuide() throws Exception {
        Tenant tenant = createTenant();
        User teacher = new User(
                "prof-setup@ecole.example", passwordEncoder.encode("Secret123!"), "Modibo", "Keita", Role.TEACHER);
        teacher.setSchoolId(tenant.getId());
        userRepository.save(teacher);

        MvcResult login = mockMvc.perform(post("/api/v1/auth/login")
                        .header(TenantResolver.TENANT_HEADER, tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TenantLoginRequest(
                                tenant.getSubdomain(), "prof-setup@ecole.example", "Secret123!"))))
                .andExpect(status().isOk())
                .andReturn();
        String token = objectMapper.readTree(login.getResponse().getContentAsString())
                .get("data").get("accessToken").asText();

        mockMvc.perform(get("/api/v1/onboarding/status")
                        .header("Authorization", "Bearer " + token)
                        .header(TenantResolver.TENANT_HEADER, tenant.getId()))
                .andExpect(status().isForbidden());
    }
}
