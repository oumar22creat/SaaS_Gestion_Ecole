package com.schoolsaas.billing;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Feature flags par plan (ROADMAP.md 3.7, cahier §4.1) : {@link PlanFeatureInterceptor} bloque
 * Cantine/Transport/Bibliothèque tant que le plan souscrit ne les inclut pas — testé ici
 * indépendamment des modules eux-mêmes (déjà couverts, avec le plan Premium accordé via
 * {@link TestAuthSupport#grantAllPlanFeatures}, par CanteenTest/TransportTest/LibraryTest).
 */
class PlanFeatureInterceptorTest extends AbstractIntegrationTest {

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
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PlanRepository planRepository;

    @Test
    void blocksCanteenTransportAndLibraryWithoutASubscriptionThatIncludesThem() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Sans Options");
        String token = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "secretary-nofeature@ecole.example", Role.SECRETARY);

        mockMvc.perform(get("/api/v1/transport/routes").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FEATURE_NOT_INCLUDED"));

        mockMvc.perform(get("/api/v1/canteen/menus?from=2026-10-01&to=2026-10-31").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FEATURE_NOT_INCLUDED"));

        mockMvc.perform(get("/api/v1/library/books?query=Ada").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FEATURE_NOT_INCLUDED"));

        TestAuthSupport.grantAllPlanFeatures(subscriptionRepository, planRepository, tenant);

        mockMvc.perform(get("/api/v1/transport/routes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/canteen/menus?from=2026-10-01&to=2026-10-31").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/library/books?query=Ada").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
