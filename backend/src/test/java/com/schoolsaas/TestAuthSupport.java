package com.schoolsaas;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolsaas.auth.Role;
import com.schoolsaas.auth.User;
import com.schoolsaas.auth.UserRepository;
import com.schoolsaas.auth.dto.LoginRequest;
import com.schoolsaas.billing.Plan;
import com.schoolsaas.billing.PlanRepository;
import com.schoolsaas.billing.Subscription;
import com.schoolsaas.billing.SubscriptionRepository;
import com.schoolsaas.billing.SubscriptionStatus;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantRepository;
import com.schoolsaas.tenant.TenantResolver;
import com.schoolsaas.tenant.TenantStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

/**
 * Fabrique un tenant ACTIVE + un utilisateur + un access token, pour les tests d'intégration
 * des modules métier (1.5+) qui n'ont pas besoin de repasser par le flux d'inscription/essai
 * complet (voir BillingSubscriptionTest pour ce cas-là). Regroupé ici pour éviter de dupliquer
 * ce câblage dans chaque nouvelle classe de test (voir CLAUDE.md : pas de duplication évitable).
 */
public final class TestAuthSupport {

    private TestAuthSupport() {
    }

    public static Tenant createActiveTenant(TenantRepository tenantRepository, String label) {
        return tenantRepository.save(new Tenant(label, "tenant-" + UUID.randomUUID(), TenantStatus.ACTIVE));
    }

    /**
     * Un tenant créé par {@link #createActiveTenant} n'a par défaut aucun abonnement — suffisant
     * pour la plupart des tests, mais {@code PlanFeatureInterceptor} (ROADMAP.md 3.7) bloque en
     * conséquence les modules Cantine/Transport/Bibliothèque (aucun abonnement = aucune
     * fonctionnalité incluse, fail-closed). À appeler pour les tests de ces modules qui doivent
     * passer par une vraie requête HTTP plutôt que d'accéder aux repositories directement.
     */
    public static void grantAllPlanFeatures(SubscriptionRepository subscriptionRepository, PlanRepository planRepository, Tenant tenant) {
        Plan premium = planRepository.findByCode("PREMIUM")
                .orElseThrow(() -> new IllegalStateException("Plan PREMIUM introuvable (voir V4__create_plans_table.sql)"));
        subscriptionRepository.save(new Subscription(tenant.getId(), premium.getId(), SubscriptionStatus.ACTIVE, Instant.now()));
    }

    /**
     * Positionne school_id avant la persistance d'une fixture de test créée hors requête HTTP
     * (donc sans TenantContext courant, voir TenantScopedEntity#assignTenantIfMissing) — à
     * appeler avant tout {@code repository.save(...)} de données appartenant à un tenant B
     * dans un test d'isolation.
     */
    public static <T extends com.schoolsaas.common.TenantScopedEntity> T withTenant(T entity, Long tenantId) {
        entity.setSchoolId(tenantId);
        return entity;
    }

    public static User createUser(
            UserRepository userRepository, PasswordEncoder passwordEncoder, Tenant tenant, String email, Role role) {
        User user = new User(email, passwordEncoder.encode("Sup3rSecret!"), "Ada", "Lovelace", role);
        user.setSchoolId(tenant.getId());
        return userRepository.save(user);
    }

    public static String login(MockMvc mockMvc, ObjectMapper objectMapper, Tenant tenant, String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .header(TenantResolver.TENANT_HEADER, tenant.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "Sup3rSecret!"))))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
        return data.get("accessToken").asText();
    }

    public static String createUserAndLogin(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            Tenant tenant,
            String email,
            Role role) throws Exception {
        createUser(userRepository, passwordEncoder, tenant, email, role);
        return login(mockMvc, objectMapper, tenant, email);
    }
}
