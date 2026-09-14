package com.schoolsaas.billing;

import com.stripe.model.Event;

/**
 * Frontière avec l'API Stripe — interface séparée de {@link StripeGatewayImpl} uniquement
 * pour permettre aux tests (JUnit + Mockito, voir CLAUDE.md règle 4) de simuler Stripe sans
 * appel réseau réel ; {@link BillingCheckoutService} et {@link StripeWebhookService} ne
 * connaissent que cette interface.
 */
public interface StripeGateway {

    /** Crée un Customer Stripe si {@code existingCustomerId} est null, sinon le réutilise. */
    String resolveCustomerId(String existingCustomerId, String email, Long tenantId);

    CheckoutSession createCheckoutSession(
            String customerId, String priceId, Long tenantId, String planCode, String successUrl, String cancelUrl);

    /** Vérifie la signature Stripe et décode l'événement — lève ApiException si invalide. */
    Event parseWebhookEvent(String payload, String signatureHeader);

    record CheckoutSession(String id, String url) {
    }
}
