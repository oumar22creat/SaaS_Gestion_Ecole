package com.schoolsaas.billing;

import org.springframework.stereotype.Service;

/** Vérifie si un tenant a accès à une fonctionnalité selon son plan (cahier-des-charges.md §4.1, ROADMAP.md 3.7). */
@Service
public class PlanFeatureService {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;

    public PlanFeatureService(SubscriptionRepository subscriptionRepository, PlanRepository planRepository) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
    }

    public boolean tenantHasFeature(Long tenantId, PlanFeature feature) {
        return subscriptionRepository.findByTenantId(tenantId)
                .flatMap(subscription -> planRepository.findById(subscription.getPlanId()))
                .map(plan -> hasFeature(plan, feature))
                .orElse(false);
    }

    private boolean hasFeature(Plan plan, PlanFeature feature) {
        return switch (feature) {
            case CANTEEN -> plan.isCanteenIncluded();
            case TRANSPORT -> plan.isTransportIncluded();
            case LIBRARY -> plan.isLibraryIncluded();
            case CUSTOM_DOMAIN -> plan.isCustomDomainIncluded();
        };
    }
}
