package com.schoolsaas.statistics;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolsaas.AbstractIntegrationTest;
import com.schoolsaas.TestAuthSupport;
import com.schoolsaas.auth.PlatformAdmin;
import com.schoolsaas.auth.PlatformAdminRepository;
import com.schoolsaas.auth.Role;
import com.schoolsaas.auth.UserRepository;
import com.schoolsaas.auth.dto.LoginRequest;
import com.schoolsaas.billing.Plan;
import com.schoolsaas.billing.PlanRepository;
import com.schoolsaas.billing.Subscription;
import com.schoolsaas.billing.SubscriptionRepository;
import com.schoolsaas.billing.SubscriptionStatus;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantRepository;
import com.schoolsaas.tenant.TenantStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/** Tableau de bord Super-Admin (ROADMAP.md 2.6) : agrégats plateforme, RBAC. */
class PlatformDashboardTest extends AbstractIntegrationTest {

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

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Test
    void aSuperAdminSeesTenantCountsAndMrr() throws Exception {
        Tenant trial = tenantRepository.save(new Tenant("École Essai", "tenant-trial-" + java.util.UUID.randomUUID(), TenantStatus.TRIAL));
        Tenant active = tenantRepository.save(new Tenant("École Active", "tenant-active-" + java.util.UUID.randomUUID(), TenantStatus.ACTIVE));
        Tenant suspended = tenantRepository.save(
                new Tenant("École Suspendue", "tenant-suspended-" + java.util.UUID.randomUUID(), TenantStatus.SUSPENDED));

        Plan plan = planRepository.findByCode("STANDARD")
                .orElseThrow(() -> new IllegalStateException("Plan STANDARD introuvable (voir V4/données de test)"));

        subscriptionRepository.save(new Subscription(trial.getId(), plan.getId(), SubscriptionStatus.TRIALING, Instant.now().plusSeconds(3600)));
        subscriptionRepository.save(new Subscription(active.getId(), plan.getId(), SubscriptionStatus.ACTIVE, Instant.now().minusSeconds(3600)));
        subscriptionRepository.save(new Subscription(suspended.getId(), plan.getId(), SubscriptionStatus.CANCELED, Instant.now().minusSeconds(3600)));

        String adminToken = createPlatformAdminAndLogin("super-dashboard@schoolsaas.example");

        mockMvc.perform(get("/api/v1/admin/dashboard/summary").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.trialCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.activeCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.data.cancelledCount").value(org.hamcrest.Matchers.greaterThanOrEqualTo(0)))
                .andExpect(jsonPath("$.data.mrrCents").value(org.hamcrest.Matchers.greaterThanOrEqualTo(plan.getPriceCents())))
                .andExpect(jsonPath("$.data.arrCents").value(org.hamcrest.Matchers.greaterThanOrEqualTo(plan.getPriceCents() * 12)))
                .andExpect(jsonPath("$.data.currency").value(plan.getCurrency()));
    }

    @Test
    void aRegularStaffUserCannotAccessThePlatformDashboard() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Non-Admin");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "admin-not-super@ecole.example", Role.ADMIN);

        mockMvc.perform(get("/api/v1/admin/dashboard/summary").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    private String createPlatformAdminAndLogin(String email) throws Exception {
        platformAdminRepository.save(new PlatformAdmin(email, passwordEncoder.encode("Sup3rSecret!"), "Super", "Admin"));
        var result = mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "Sup3rSecret!"))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        return data.get("accessToken").asText();
    }
}
