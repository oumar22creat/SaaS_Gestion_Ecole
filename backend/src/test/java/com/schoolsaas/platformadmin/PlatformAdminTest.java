package com.schoolsaas.platformadmin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schoolsaas.AbstractIntegrationTest;
import com.schoolsaas.TestAuthSupport;
import com.schoolsaas.auth.PlatformAdminRepository;
import com.schoolsaas.billing.Plan;
import com.schoolsaas.billing.PlanRepository;
import com.schoolsaas.billing.Subscription;
import com.schoolsaas.billing.SubscriptionRepository;
import com.schoolsaas.billing.SubscriptionStatus;
import com.schoolsaas.auth.Role;
import com.schoolsaas.auth.UserRepository;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantRepository;
import com.schoolsaas.tenant.TenantStatus;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Console plateforme (cahier-des-charges.md §5) : gérer les établissements clients sans jamais
 * accéder à leurs données scolaires.
 */
class PlatformAdminTest extends AbstractIntegrationTest {

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
    private PlatformAdminActionRepository actionRepository;

    @Autowired
    private PlanRepository planRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Test
    void listsTenantsWithTheirSubscriptionAndFiltersThem() throws Exception {
        Tenant actif = TestAuthSupport.createActiveTenant(tenantRepository, "École Filtre Active");
        Tenant suspendu = TestAuthSupport.createActiveTenant(tenantRepository, "École Filtre Suspendue");
        suspendu.setStatus(TenantStatus.SUSPENDED);
        tenantRepository.save(suspendu);
        String token = superAdminToken("console-list@platform.example");

        // Sans aucun critère : c'est le premier affichage de l'écran, et le cas où une
        // requête à paramètres nuls faisait échouer PostgreSQL sur « lower(bytea) ».
        mockMvc.perform(get("/api/v1/admin/tenants").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.meta.total").isNumber());

        // La recherche porte sur le nom comme sur le sous-domaine.
        mockMvc.perform(get("/api/v1/admin/tenants")
                        .param("search", "Filtre Active")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value(actif.getName()))
                .andExpect(jsonPath("$.data[0].subdomain").exists());

        // Le filtre de statut se combine à la recherche. Les deux critères sont posés ensemble
        // ici : d'autres tests de la suite créent aussi des établissements suspendus, et une
        // assertion sur leur nombre total dépendrait de l'ordre d'exécution.
        mockMvc.perform(get("/api/v1/admin/tenants")
                        .param("status", "SUSPENDED")
                        .param("search", "École Filtre")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value(suspendu.getName()));

        // Et le statut exclut bien l'actif, qui porte pourtant le même terme de recherche.
        mockMvc.perform(get("/api/v1/admin/tenants")
                        .param("status", "ACTIVE")
                        .param("search", "École Filtre")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].name").value(actif.getName()));
    }

    /**
     * Le cœur de la promesse faite aux établissements : l'éditeur gère l'abonnement de ses
     * clients, il ne lit pas le dossier scolaire de leurs élèves. Row-Level Security le
     * garantit en base ; ce test vérifie que la console n'ouvre pas de porte dérobée.
     */
    @Test
    void aSuperAdminNeverReachesPupilData() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Cloison");
        TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "admin-cloison@ecole.example", Role.ADMIN);
        String token = superAdminToken("console-cloison@platform.example");

        for (String path : java.util.List.of(
                "/api/v1/students", "/api/v1/parents", "/api/v1/users", "/api/v1/report-cards", "/api/v1/school-fees/summary")) {
            mockMvc.perform(get(path).header("Authorization", "Bearer " + token))
                    .andExpect(status().isForbidden());
        }
    }

    /**
     * Couper l'accès d'un établissement entier ne doit jamais être un geste anonyme : sans
     * motif, personne ne peut répondre six mois plus tard à « pourquoi cette école est-elle
     * coupée ? ».
     */
    @Test
    void suspendingATenantRequiresAReasonAndIsRecorded() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Suspension");
        String token = superAdminToken("console-suspend@platform.example");

        mockMvc.perform(put("/api/v1/admin/tenants/" + tenant.getId() + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUSPENDED\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error.code").value("REASON_REQUIRED"));

        mockMvc.perform(put("/api/v1/admin/tenants/" + tenant.getId() + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"SUSPENDED\",\"reason\":\"Impayé depuis 3 mois\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUSPENDED"));

        mockMvc.perform(get("/api/v1/admin/tenants/" + tenant.getId() + "/actions")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(1))
                .andExpect(jsonPath("$.data[0].action").value("TENANT_STATUS_CHANGED"))
                .andExpect(jsonPath("$.data[0].detail").value("ACTIVE → SUSPENDED"))
                .andExpect(jsonPath("$.data[0].reason").value("Impayé depuis 3 mois"));

        // Réactiver n'exige pas de motif : rendre l'accès ne restreint rien.
        mockMvc.perform(put("/api/v1/admin/tenants/" + tenant.getId() + "/status")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVE\"}"))
                .andExpect(status().isOk());
    }

    /**
     * Prolonger un essai déjà expiré doit le rendre à nouveau utilisable. En repartant de la
     * date d'échéance passée, « +15 jours » aurait laissé l'établissement expiré.
     */
    @Test
    void extendingAnExpiredTrialReopensAccessFromToday() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Essai Expiré");
        tenant.setStatus(TenantStatus.SUSPENDED);
        tenantRepository.save(tenant);
        // Essai échu depuis un mois : c'est le cas que la prolongation doit rattraper.
        Plan plan = planRepository.findAll().getFirst();
        subscriptionRepository.save(new Subscription(
                tenant.getId(), plan.getId(), SubscriptionStatus.TRIALING,
                Instant.now().minus(30, java.time.temporal.ChronoUnit.DAYS)));
        String token = superAdminToken("console-essai@platform.example");

        mockMvc.perform(put("/api/v1/admin/tenants/" + tenant.getId() + "/trial")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"days\":15,\"reason\":\"Geste commercial\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("TRIAL"));

        assertThat(actionRepository.findAllByTenantIdOrderByCreatedAtDesc(tenant.getId()))
                .anyMatch(action -> action.getAction().equals("TRIAL_EXTENDED"));

        // Le nouveau terme part d'aujourd'hui, pas de la date échue : sinon l'établissement
        // resterait expiré après une prolongation de 15 jours.
        assertThat(subscriptionRepository.findByTenantId(tenant.getId()).orElseThrow().getTrialEndsAt())
                .isAfter(Instant.now());
    }

    /**
     * Le règlement arrive en espèces : c'est le Super-Administrateur qui l'enregistre, et cet
     * enregistrement est ce qui rouvre l'accès d'un établissement expiré. Sans lui, une école
     * qui a payé resterait coupée.
     */
    @Test
    void recordingACashPaymentReopensAccessAndExtendsThePeriod() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Règlement Espèces");
        tenant.setStatus(TenantStatus.SUSPENDED);
        tenantRepository.save(tenant);
        Plan plan = planRepository.findByCode("ESSENTIEL").orElseThrow();
        Subscription subscription = subscriptionRepository.save(new Subscription(
                tenant.getId(), plan.getId(), SubscriptionStatus.EXPIRED,
                Instant.now().minus(400, java.time.temporal.ChronoUnit.DAYS)));
        subscription.setCurrentPeriodEnd(Instant.now().minus(10, java.time.temporal.ChronoUnit.DAYS));
        subscriptionRepository.save(subscription);
        String token = superAdminToken("console-especes@platform.example");

        mockMvc.perform(post("/api/v1/admin/tenants/" + tenant.getId() + "/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":" + plan.getId() + ",\"months\":12,\"reason\":\"Reçu n°42\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.subscriptionStatus").value("ACTIVE"));

        // L'échéance repart d'aujourd'hui : elle était passée, la faire courir depuis la date
        // échue aurait vendu dix jours déjà écoulés.
        assertThat(subscriptionRepository.findByTenantId(tenant.getId()).orElseThrow().getCurrentPeriodEnd())
                .isAfter(Instant.now().plus(360, java.time.temporal.ChronoUnit.DAYS));

        // Le journal tient lieu de reçu : montant attendu et nouvelle échéance y figurent.
        assertThat(actionRepository.findAllByTenantIdOrderByCreatedAtDesc(tenant.getId()))
                .anyMatch(action -> action.getAction().equals("CASH_PAYMENT_RECORDED")
                        && action.getDetail().contains("12 mois")
                        && action.getDetail().contains(String.valueOf(plan.getPriceCents() * 12)));
    }

    /** Régler en avance ne doit pas faire perdre les jours déjà payés. */
    @Test
    void anEarlyPaymentStacksOnTopOfTheRunningPeriod() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Règlement Anticipé");
        Plan plan = planRepository.findByCode("ESSENTIEL").orElseThrow();
        Subscription subscription = subscriptionRepository.save(new Subscription(
                tenant.getId(), plan.getId(), SubscriptionStatus.ACTIVE,
                Instant.now().minus(40, java.time.temporal.ChronoUnit.DAYS)));
        Instant runningUntil = Instant.now().plus(60, java.time.temporal.ChronoUnit.DAYS);
        subscription.setCurrentPeriodEnd(runningUntil);
        subscriptionRepository.save(subscription);
        String token = superAdminToken("console-anticipe@platform.example");

        mockMvc.perform(post("/api/v1/admin/tenants/" + tenant.getId() + "/payments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"planId\":" + plan.getId() + ",\"months\":3}"))
                .andExpect(status().isOk());

        assertThat(subscriptionRepository.findByTenantId(tenant.getId()).orElseThrow().getCurrentPeriodEnd())
                .isAfter(runningUntil.plus(85, java.time.temporal.ChronoUnit.DAYS));
    }

    /** Un administrateur d'établissement n'a rien à faire dans la console plateforme. */
    @Test
    void aTenantAdminCannotReachThePlatformConsole() throws Exception {
        Tenant tenant = TestAuthSupport.createActiveTenant(tenantRepository, "École Hors Console");
        String tenantAdminToken = TestAuthSupport.createUserAndLogin(
                mockMvc, objectMapper, userRepository, passwordEncoder, tenant, "admin-hors@ecole.example", Role.ADMIN);

        for (String path : java.util.List.of("/api/v1/admin/tenants", "/api/v1/admin/plans", "/api/v1/admin/accounts")) {
            mockMvc.perform(get(path).header("Authorization", "Bearer " + tenantAdminToken))
                    .andExpect(status().isForbidden());
        }
    }

    /** Un second compte plateforme doit pouvoir être créé depuis la console, et se connecter. */
    @Test
    void createsAnotherPlatformAdminThatCanSignIn() throws Exception {
        String token = superAdminToken("console-compte@platform.example");

        mockMvc.perform(post("/api/v1/admin/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"collegue@platform.example\",\"password\":\"MotDePasseTresLong1!\","
                                + "\"firstName\":\"Awa\",\"lastName\":\"Traoré\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.email").value("collegue@platform.example"));

        mockMvc.perform(post("/api/v1/admin/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"collegue@platform.example\",\"password\":\"MotDePasseTresLong1!\"}"))
                .andExpect(status().isOk());

        // Un mot de passe court est refusé : ce compte voit tous les établissements.
        mockMvc.perform(post("/api/v1/admin/accounts")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"faible@platform.example\",\"password\":\"court1!\","
                                + "\"firstName\":\"A\",\"lastName\":\"B\"}"))
                .andExpect(status().isBadRequest());
    }

    private String superAdminToken(String email) throws Exception {
        return TestAuthSupport.createPlatformAdminAndLogin(
                mockMvc, objectMapper, platformAdminRepository, passwordEncoder, email);
    }
}
