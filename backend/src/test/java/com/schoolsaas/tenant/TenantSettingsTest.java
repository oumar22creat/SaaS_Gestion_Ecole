package com.schoolsaas.tenant;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolsaas.AbstractIntegrationTest;
import com.schoolsaas.TestAuthSupport;
import com.schoolsaas.auth.Role;
import com.schoolsaas.auth.UserRepository;
import com.schoolsaas.billing.PlanRepository;
import com.schoolsaas.billing.SubscriptionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Personnalisation par établissement (ROADMAP.md 3.7) : branding, modèle de bulletin, domaine
 * personnalisé (gate plan Premium), RBAC et isolation cross-tenant.
 */
class TenantSettingsTest extends AbstractIntegrationTest {

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
    void brandingIsPublicByDefaultAndReflectsAdminUpdates() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Branding");

        mockMvc.perform(get("/api/v1/tenants/current/branding").header(TenantResolver.TENANT_HEADER, tenant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryColor").value("#0f5c4c"))
                .andExpect(jsonPath("$.logoUrl").doesNotExist());

        String adminToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "admin-branding@ecole.example", Role.ADMIN);
        String secretaryToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "secretary-branding@ecole.example", Role.SECRETARY);

        mockMvc.perform(put("/api/v1/tenants/current/branding")
                        .header("Authorization", "Bearer " + secretaryToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"logoUrl\":\"https://cdn.example.com/logo.png\",\"primaryColor\":\"#ff0000\",\"secondaryColor\":\"#00ff00\"}"))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/tenants/current/branding")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"logoUrl\":\"https://cdn.example.com/logo.png\",\"primaryColor\":\"not-a-color\",\"secondaryColor\":\"#00ff00\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));

        mockMvc.perform(put("/api/v1/tenants/current/branding")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"logoUrl\":\"https://cdn.example.com/logo.png\",\"primaryColor\":\"#ff0000\",\"secondaryColor\":\"#00ff00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.primaryColor").value("#ff0000"));

        mockMvc.perform(get("/api/v1/tenants/current/branding").header(TenantResolver.TENANT_HEADER, tenant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoUrl").value("https://cdn.example.com/logo.png"))
                .andExpect(jsonPath("$.secondaryColor").value("#00ff00"));
    }

    @Test
    void reportCardTemplateRequiresAdminOrDirectionRole() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Bulletin");
        String directionToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "direction-template@ecole.example", Role.DIRECTION);
        String secretaryToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "secretary-template@ecole.example", Role.SECRETARY);

        mockMvc.perform(get("/api/v1/tenants/current/report-card-template").header("Authorization", "Bearer " + secretaryToken))
                .andExpect(status().isForbidden());

        mockMvc.perform(put("/api/v1/tenants/current/report-card-template")
                        .header("Authorization", "Bearer " + directionToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reportCardHeader\":\"Collège Voltaire\",\"reportCardLegalMentions\":\"Établissement privé sous contrat\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportCardHeader").value("Collège Voltaire"));

        mockMvc.perform(get("/api/v1/tenants/current/report-card-template").header("Authorization", "Bearer " + directionToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.reportCardLegalMentions").value("Établissement privé sous contrat"));
    }

    @Test
    void customDomainRequiresThePremiumPlanFeature() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Domaine");
        String adminToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "admin-domain@ecole.example", Role.ADMIN);

        mockMvc.perform(put("/api/v1/tenants/current/custom-domain")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customDomain\":\"ecole-voltaire.example.com\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("FEATURE_NOT_INCLUDED"));

        TestAuthSupport.grantAllPlanFeatures(subscriptionRepository, planRepository, tenant);

        mockMvc.perform(put("/api/v1/tenants/current/custom-domain")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customDomain\":\"ecole-voltaire.example.com\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customDomain").value("ecole-voltaire.example.com"));

        mockMvc.perform(get("/api/v1/tenants/current/custom-domain").header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customDomain").value("ecole-voltaire.example.com"));
    }

    @Test
    void aTenantsBrandingAndCustomDomainNeverLeakToAnotherTenant() throws Exception {
        Tenant tenantA = TestAuthSupport.createActiveTenant(tenantRepository, "École Isolation A");
        Tenant tenantB = TestAuthSupport.createActiveTenant(tenantRepository, "École Isolation B");
        TestAuthSupport.grantAllPlanFeatures(subscriptionRepository, planRepository, tenantA);
        String adminAToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenantA, "admin-iso-a@ecole.example", Role.ADMIN);

        mockMvc.perform(put("/api/v1/tenants/current/branding")
                        .header("Authorization", "Bearer " + adminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"logoUrl\":\"https://cdn.example.com/secret-logo-a.png\",\"primaryColor\":\"#123456\",\"secondaryColor\":\"#654321\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(put("/api/v1/tenants/current/custom-domain")
                        .header("Authorization", "Bearer " + adminAToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"customDomain\":\"ecole-isolation-a.example.com\"}"))
                .andExpect(status().isOk());

        // Tenant B, résolu par en-tête, ne doit jamais voir le branding de A.
        mockMvc.perform(get("/api/v1/tenants/current/branding").header(TenantResolver.TENANT_HEADER, tenantB.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.primaryColor").value("#0f5c4c"));

        // Le domaine personnalisé de A, résolu par nom d'hôte (sans en-tête X-Tenant-Id), doit
        // renvoyer exactement le branding de A — jamais celui d'un autre tenant.
        mockMvc.perform(get("/api/v1/tenants/current/branding")
                        .with(request -> {
                            request.setServerName("ecole-isolation-a.example.com");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.logoUrl").value("https://cdn.example.com/secret-logo-a.png"))
                .andExpect(jsonPath("$.primaryColor").value("#123456"));
    }
}
