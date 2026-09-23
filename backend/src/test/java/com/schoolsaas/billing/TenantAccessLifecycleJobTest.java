package com.schoolsaas.billing;

import static org.assertj.core.api.Assertions.assertThat;

import com.schoolsaas.AbstractIntegrationTest;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantRepository;
import com.schoolsaas.tenant.TenantStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Vérifie le blocage progressif (cahier-des-charges.md §4.2) piloté par
 * {@link TenantAccessLifecycleJob}, avec les délais de grâce par défaut de test (identiques
 * à application.yml : 3 jours après échec de paiement, 7 jours de lecture seule avant
 * suspension — voir docs/ARCHITECTURE.md ADR-009).
 */
class TenantAccessLifecycleJobTest extends AbstractIntegrationTest {

    @Autowired
    private TenantAccessLifecycleJob job;

    @Autowired
    private TenantRepository tenantRepository;

    @Autowired
    private SubscriptionRepository subscriptionRepository;

    @Autowired
    private PlanRepository planRepository;

    private Long essentielPlanId;

    private Long essentielPlanId() {
        if (essentielPlanId == null) {
            essentielPlanId = planRepository.findByCode("ESSENTIEL").orElseThrow().getId();
        }
        return essentielPlanId;
    }

    private Tenant createTenant(String label, TenantStatus status) {
        return tenantRepository.save(new Tenant(label, "tenant-" + UUID.randomUUID(), status));
    }

    @Test
    void expiredTrialWithoutPaymentBecomesReadOnlyThenSuspendedAcrossTwoRuns() {
        Tenant tenant = createTenant("Essai expiré", TenantStatus.TRIAL);
        subscriptionRepository.save(new Subscription(
                tenant.getId(), essentielPlanId(), SubscriptionStatus.TRIALING, Instant.now().minusSeconds(60)));

        // Juste après la fin d'essai : passe en lecture seule, pas directement suspendu.
        job.evaluate(Instant.now());
        assertThat(tenantRepository.findById(tenant.getId()).orElseThrow().getStatus()).isEqualTo(TenantStatus.READ_ONLY);

        // Bien après (au-delà des 7 jours de grâce en lecture seule) : suspendu à l'exécution suivante.
        Instant farInTheFuture = Instant.now().plus(Duration.ofDays(8));
        job.evaluate(farInTheFuture);
        assertThat(tenantRepository.findById(tenant.getId()).orElseThrow().getStatus()).isEqualTo(TenantStatus.SUSPENDED);
    }

    @Test
    void trialStillRunningIsUntouched() {
        Tenant tenant = createTenant("Essai en cours", TenantStatus.TRIAL);
        subscriptionRepository.save(new Subscription(
                tenant.getId(), essentielPlanId(), SubscriptionStatus.TRIALING, Instant.now().plus(Duration.ofDays(10))));

        job.evaluate(Instant.now());

        assertThat(tenantRepository.findById(tenant.getId()).orElseThrow().getStatus()).isEqualTo(TenantStatus.TRIAL);
    }

    @Test
    void paymentFailureWithinGraceDoesNotDowngradeYet() {
        Tenant tenant = createTenant("Échec récent", TenantStatus.ACTIVE);
        Subscription subscription = subscriptionRepository.save(new Subscription(
                tenant.getId(), essentielPlanId(), SubscriptionStatus.PAST_DUE, Instant.now().minusSeconds(60)));
        subscription.setPaymentFailedAt(Instant.now().minus(Duration.ofDays(1)));
        subscriptionRepository.save(subscription);

        job.evaluate(Instant.now());

        assertThat(tenantRepository.findById(tenant.getId()).orElseThrow().getStatus()).isEqualTo(TenantStatus.ACTIVE);
    }

    @Test
    void paymentFailurePastGraceBecomesReadOnlyThenSuspended() {
        Tenant tenant = createTenant("Échec ancien", TenantStatus.ACTIVE);
        Subscription subscription = subscriptionRepository.save(new Subscription(
                tenant.getId(), essentielPlanId(), SubscriptionStatus.PAST_DUE, Instant.now().minusSeconds(60)));
        subscription.setPaymentFailedAt(Instant.now().minus(Duration.ofDays(4)));
        subscriptionRepository.save(subscription);

        // 4 jours > 3 jours de grâce "past due" : passe en lecture seule.
        job.evaluate(Instant.now());
        assertThat(tenantRepository.findById(tenant.getId()).orElseThrow().getStatus()).isEqualTo(TenantStatus.READ_ONLY);

        // Bien plus tard (au-delà de 3+7 jours depuis l'échec) : suspendu.
        Instant farInTheFuture = Instant.now().plus(Duration.ofDays(10));
        job.evaluate(farInTheFuture);
        assertThat(tenantRepository.findById(tenant.getId()).orElseThrow().getStatus()).isEqualTo(TenantStatus.SUSPENDED);
    }

    /**
     * Cas du règlement en espèces : la période payée s'achève, l'accès se ferme le jour dit.
     * Pas d'étape en lecture seule ici, contrairement aux deux scénarios ci-dessus — voir
     * {@code TenantAccessLifecycleJob#expireCashSubscriptions}.
     */
    @Test
    void cashSubscriptionPastItsPeriodIsSuspendedAndMarkedExpired() {
        Tenant tenant = createTenant("Espèces échu", TenantStatus.ACTIVE);
        Subscription subscription = subscriptionRepository.save(new Subscription(
                tenant.getId(), essentielPlanId(), SubscriptionStatus.ACTIVE, Instant.now().minus(Duration.ofDays(400))));
        subscription.setCurrentPeriodEnd(Instant.now().minus(Duration.ofDays(1)));
        subscriptionRepository.save(subscription);

        job.evaluate(Instant.now());

        assertThat(tenantRepository.findById(tenant.getId()).orElseThrow().getStatus()).isEqualTo(TenantStatus.SUSPENDED);
        assertThat(subscriptionRepository.findById(subscription.getId()).orElseThrow().getStatus())
                .isEqualTo(SubscriptionStatus.EXPIRED);
    }

    @Test
    void cashSubscriptionStillCoveredIsUntouched() {
        Tenant tenant = createTenant("Espèces en cours", TenantStatus.ACTIVE);
        Subscription subscription = subscriptionRepository.save(new Subscription(
                tenant.getId(), essentielPlanId(), SubscriptionStatus.ACTIVE, Instant.now().minus(Duration.ofDays(40))));
        subscription.setCurrentPeriodEnd(Instant.now().plus(Duration.ofDays(30)));
        subscriptionRepository.save(subscription);

        job.evaluate(Instant.now());

        assertThat(tenantRepository.findById(tenant.getId()).orElseThrow().getStatus()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(subscriptionRepository.findById(subscription.getId()).orElseThrow().getStatus())
                .isEqualTo(SubscriptionStatus.ACTIVE);
    }

    /**
     * Un abonnement Stripe a sa propre fin de période, que le webhook repousse à chaque
     * prélèvement réussi. L'expirer ici couperait un client parfaitement à jour, le temps que
     * l'événement de renouvellement arrive.
     */
    @Test
    void stripeSubscriptionPastItsPeriodIsLeftToTheWebhook() {
        Tenant tenant = createTenant("Stripe en cours", TenantStatus.ACTIVE);
        Subscription subscription = subscriptionRepository.save(new Subscription(
                tenant.getId(), essentielPlanId(), SubscriptionStatus.ACTIVE, Instant.now().minus(Duration.ofDays(40))));
        subscription.setCurrentPeriodEnd(Instant.now().minus(Duration.ofDays(2)));
        subscription.setStripeSubscriptionId("sub_" + UUID.randomUUID());
        subscriptionRepository.save(subscription);

        job.evaluate(Instant.now());

        assertThat(tenantRepository.findById(tenant.getId()).orElseThrow().getStatus()).isEqualTo(TenantStatus.ACTIVE);
        assertThat(subscriptionRepository.findById(subscription.getId()).orElseThrow().getStatus())
                .isEqualTo(SubscriptionStatus.ACTIVE);
    }
}
