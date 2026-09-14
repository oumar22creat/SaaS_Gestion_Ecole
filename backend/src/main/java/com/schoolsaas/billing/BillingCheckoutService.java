package com.schoolsaas.billing;

import com.schoolsaas.common.ApiException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Création de session Stripe Checkout pour souscrire/réactiver un abonnement. */
@Service
public class BillingCheckoutService {

    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final StripeGateway stripeGateway;
    private final StripeProperties stripeProperties;

    public BillingCheckoutService(
            SubscriptionRepository subscriptionRepository,
            PlanRepository planRepository,
            StripeGateway stripeGateway,
            StripeProperties stripeProperties) {
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.stripeGateway = stripeGateway;
        this.stripeProperties = stripeProperties;
    }

    @Transactional
    public StripeGateway.CheckoutSession createCheckoutSession(Long tenantId, String planCode, String adminEmail) {
        Plan plan = planRepository.findByCode(planCode)
                .orElseThrow(() -> ApiException.notFound("PLAN_NOT_FOUND", "Plan introuvable"));
        if (plan.getStripePriceId() == null) {
            throw ApiException.unprocessable("PLAN_NOT_PURCHASABLE", "Ce plan n'est pas disponible à l'achat en ligne");
        }
        Subscription subscription = subscriptionRepository.findByTenantId(tenantId)
                .orElseThrow(() -> ApiException.notFound("SUBSCRIPTION_NOT_FOUND", "Abonnement introuvable"));

        String customerId = stripeGateway.resolveCustomerId(subscription.getStripeCustomerId(), adminEmail, tenantId);
        if (!customerId.equals(subscription.getStripeCustomerId())) {
            subscription.setStripeCustomerId(customerId);
            subscriptionRepository.save(subscription);
        }

        return stripeGateway.createCheckoutSession(
                customerId,
                plan.getStripePriceId(),
                tenantId,
                plan.getCode(),
                stripeProperties.checkoutSuccessUrl(),
                stripeProperties.checkoutCancelUrl());
    }
}
