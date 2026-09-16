package com.schoolsaas.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
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
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Checkout Stripe avec {@link StripeGateway} mocké (voir docs/ARCHITECTURE.md ADR-009) : pas
 * d'appel réseau réel vers Stripe dans les tests, seule frontière mockée.
 */
class BillingCheckoutTest extends AbstractIntegrationTest {

    @MockBean
    private StripeGateway stripeGateway;

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
    void checkoutCreatesAStripeSessionAndPersistsTheCustomerIdOnceThePlanIsPurchasable() throws Exception {
        // STANDARD, pas ESSENTIEL : ESSENTIEL est le plan par défaut de l'essai gratuit et
        // d'autres tests (voir BillingSubscriptionTest) s'appuient sur le fait qu'il reste
        // sans stripe_price_id — la base Postgres de test est un singleton partagé entre
        // classes (voir AbstractIntegrationTest), donc modifier ESSENTIEL ici fuiterait vers
        // ces autres tests.
        Plan plan = planRepository.findByCode("STANDARD").orElseThrow();
        plan.setStripePriceId("price_test_standard");
        planRepository.save(plan);

        Tenant tenant = tenantRepository.save(new Tenant("École Checkout", "tenant-" + UUID.randomUUID(), TenantStatus.TRIAL));
        subscriptionRepository.save(new Subscription(
                tenant.getId(), plan.getId(), SubscriptionStatus.TRIALING, Instant.now().plus(30, ChronoUnit.DAYS)));
        User admin = new User(
                "admin-checkout@ecole.example", passwordEncoder.encode("Sup3rSecret!"), "Ada", "Lovelace", Role.ADMIN);
        admin.setSchoolId(tenant.getId());
        userRepository.save(admin);

        when(stripeGateway.resolveCustomerId(org.mockito.ArgumentMatchers.isNull(), anyString(), anyLong()))
                .thenReturn("cus_new_123");
        when(stripeGateway.createCheckoutSession(anyString(), anyString(), anyLong(), anyString(), anyString(), anyString()))
                .thenReturn(new StripeGateway.CheckoutSession("cs_test_123", "https://checkout.stripe.test/cs_test_123"));

        String accessToken = login(tenant, "admin-checkout@ecole.example");

        mockMvc.perform(post("/api/v1/billing/checkout")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CheckoutRequestBody("STANDARD"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.checkoutUrl").value("https://checkout.stripe.test/cs_test_123"));

        Subscription reloaded = subscriptionRepository.findByTenantId(tenant.getId()).orElseThrow();
        assertThat(reloaded.getStripeCustomerId()).isEqualTo("cus_new_123");
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

    private record CheckoutRequestBody(String planCode) {
    }
}
