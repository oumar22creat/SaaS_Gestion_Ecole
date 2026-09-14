package com.schoolsaas.billing;

import com.schoolsaas.common.ApiException;
import java.time.Duration;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Gère le cycle de vie "facturation" d'un abonnement (voir {@link Subscription}). */
@Service
public class SubscriptionService {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final BillingProperties billingProperties;

    public SubscriptionService(
            SubscriptionRepository subscriptionRepository,
            PlanRepository planRepository,
            BillingProperties billingProperties) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.billingProperties = billingProperties;
    }

    /**
     * Essai gratuit automatique à la création du tenant (cahier-des-charges.md §4.2) : aucun
     * choix de plan n'est demandé à l'inscription (l'assistant de configuration qui le
     * proposerait n'est pas encore construit, voir docs/ROADMAP.md 1.3) — l'essai démarre sur
     * le plan par défaut configuré ({@code app.billing.default-trial-plan-code}).
     */
    @Transactional
    public Subscription createTrialSubscription(Long tenantId) {
        Plan defaultPlan = planRepository.findByCode(billingProperties.defaultTrialPlanCode())
                .orElseThrow(() -> new IllegalStateException(
                        "Plan par défaut introuvable : " + billingProperties.defaultTrialPlanCode()));
        Instant trialEndsAt = Instant.now().plus(Duration.ofDays(billingProperties.trialDays()));
        return subscriptionRepository.save(
                new Subscription(tenantId, defaultPlan.getId(), SubscriptionStatus.TRIALING, trialEndsAt));
    }

    public Subscription getForTenant(Long tenantId) {
        return subscriptionRepository.findByTenantId(tenantId)
                .orElseThrow(() -> ApiException.notFound("SUBSCRIPTION_NOT_FOUND", "Abonnement introuvable"));
    }
}
