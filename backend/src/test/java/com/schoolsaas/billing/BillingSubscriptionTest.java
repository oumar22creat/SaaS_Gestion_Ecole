package com.schoolsaas.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolsaas.AbstractIntegrationTest;
import com.schoolsaas.auth.Role;
import com.schoolsaas.auth.User;
import com.schoolsaas.auth.UserRepository;
import com.schoolsaas.auth.dto.TenantLoginRequest;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantRepository;
import com.schoolsaas.tenant.TenantResolver;
import com.schoolsaas.tenant.TenantStatus;
import com.schoolsaas.tenant.dto.TenantRegistrationRequest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Couvre ROADMAP.md 1.4 : tables plans/subscriptions/invoices + essai gratuit automatique.
 * Le checkout Stripe réel (mock) est testé séparément dans {@link BillingCheckoutTest}, le
 * webhook dans {@link StripeWebhookServiceTest}, le blocage progressif dans
 * {@link TenantAccessLifecycleJobTest}.
 */
class BillingSubscriptionTest extends AbstractIntegrationTest {

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
    private InvoiceRepository invoiceRepository;

    @Autowired
    private PlanRepository planRepository;

    @Test
    void registrationAutomaticallyCreatesATrialSubscriptionOnTheDefaultPlan() throws Exception {
        String subdomain = "ecole-trial-" + UUID.randomUUID().toString().substring(0, 8);
        var request = new TenantRegistrationRequest(
                "École Essai", subdomain, "admin@" + subdomain + ".example", "Sup3rSecret!", "Ada", "Lovelace");

        MvcResult result = mockMvc.perform(post("/api/v1/tenants/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        String accessToken = objectMapper.readTree(result.getResponse().getContentAsString())
                .get("data").get("tokens").get("accessToken").asText();

        mockMvc.perform(get("/api/v1/billing/subscription").header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("TRIALING"))
                .andExpect(jsonPath("$.data.planCode").value("ESSENTIEL"))
                .andExpect(result2 -> {
                    JsonNode data = objectMapper.readTree(result2.getResponse().getContentAsString()).get("data");
                    Instant trialEndsAt = Instant.parse(data.get("trialEndsAt").asText());
                    assertThat(trialEndsAt).isAfter(Instant.now().plus(29, ChronoUnit.DAYS));
                    assertThat(trialEndsAt).isBefore(Instant.now().plus(31, ChronoUnit.DAYS));
                });
    }

    @Test
    void plansAreListedPubliclyWithoutAuthentication() throws Exception {
        mockMvc.perform(get("/api/v1/billing/plans"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(3))
                .andExpect(jsonPath("$.data[?(@.code == 'ESSENTIEL')]").exists());
    }

    @Test
    void checkoutFailsWithAClearErrorWhenThePlanHasNoStripePriceConfigured() throws Exception {
        Tenant tenant = createTenantWithTrial("École Sans Stripe");
        String accessToken = createAdminAndLogin(tenant, "admin-nostripe@ecole.example");

        mockMvc.perform(post("/api/v1/billing/checkout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BillingCheckoutRequestBody("ESSENTIEL"))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("PLAN_NOT_PURCHASABLE"));
    }

    @Test
    void teacherCannotAccessBillingCheckout() throws Exception {
        Tenant tenant = createTenantWithTrial("École Enseignant");
        User teacher = new User(
                "teacher@ecole.example", passwordEncoder.encode("Sup3rSecret!"), "Ada", "Lovelace", Role.TEACHER);
        teacher.setSchoolId(tenant.getId());
        userRepository.save(teacher);
        String accessToken = login(tenant, "teacher@ecole.example");

        mockMvc.perform(post("/api/v1/billing/checkout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new BillingCheckoutRequestBody("ESSENTIEL"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void aTenantCanNeverSeeInvoicesOfAnotherTenant() throws Exception {
        Tenant tenantA = createTenantWithTrial("École Factures A");
        Tenant tenantB = createTenantWithTrial("École Factures B");
        Subscription subscriptionA = subscriptionRepository.findByTenantId(tenantA.getId()).orElseThrow();
        invoiceRepository.save(new Invoice(
                tenantA.getId(),
                subscriptionA.getId(),
                "in_tenant_a_" + UUID.randomUUID(),
                4900,
                "XOF",
                InvoiceStatus.PAID,
                Instant.now().minusSeconds(3600),
                Instant.now(),
                "https://stripe.test/invoice-a-only"));

        String accessTokenB = createAdminAndLogin(tenantB, "admin-b-invoices@ecole.example");

        mockMvc.perform(get("/api/v1/billing/invoices").header("Authorization", "Bearer " + accessTokenB))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0))
                .andExpect(result -> assertThat(result.getResponse().getContentAsString())
                        .doesNotContain("invoice-a-only"));
    }

    private Tenant createTenantWithTrial(String label) {
        Tenant tenant = tenantRepository.save(new Tenant(label, "tenant-" + UUID.randomUUID(), TenantStatus.TRIAL));
        Long essentielPlanId = planRepository.findByCode("ESSENTIEL").orElseThrow().getId();
        subscriptionRepository.save(new Subscription(
                tenant.getId(), essentielPlanId, SubscriptionStatus.TRIALING, Instant.now().plus(30, ChronoUnit.DAYS)));
        return tenant;
    }

    private String createAdminAndLogin(Tenant tenant, String email) throws Exception {
        User admin = new User(email, passwordEncoder.encode("Sup3rSecret!"), "Ada", "Lovelace", Role.ADMIN);
        admin.setSchoolId(tenant.getId());
        userRepository.save(admin);
        return login(tenant, email);
    }

    private String login(Tenant tenant, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .header(TenantResolver.TENANT_HEADER, tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TenantLoginRequest(tenant.getSubdomain(), email, "Sup3rSecret!"))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        return data.get("accessToken").asText();
    }

    private record BillingCheckoutRequestBody(String planCode) {
    }
}
