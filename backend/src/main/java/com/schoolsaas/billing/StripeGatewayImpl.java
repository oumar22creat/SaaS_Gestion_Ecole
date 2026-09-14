package com.schoolsaas.billing;

import com.schoolsaas.common.ApiException;
import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.Event;
import com.stripe.model.checkout.Session;
import com.stripe.net.Webhook;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import java.util.List;
import org.springframework.stereotype.Component;

/** Seule classe du projet à appeler directement le SDK Stripe — voir {@link StripeGateway}. */
@Component
public class StripeGatewayImpl implements StripeGateway {

    private final StripeProperties properties;

    public StripeGatewayImpl(StripeProperties properties) {
        this.properties = properties;
        Stripe.apiKey = properties.secretKey();
    }

    @Override
    public String resolveCustomerId(String existingCustomerId, String email, Long tenantId) {
        if (existingCustomerId != null) {
            return existingCustomerId;
        }
        try {
            CustomerCreateParams params = CustomerCreateParams.builder()
                    .setEmail(email)
                    .putMetadata("tenantId", String.valueOf(tenantId))
                    .build();
            return Customer.create(params).getId();
        } catch (StripeException e) {
            throw ApiException.unprocessable("STRIPE_ERROR", "Impossible de créer le client Stripe");
        }
    }

    @Override
    public CheckoutSession createCheckoutSession(
            String customerId, String priceId, Long tenantId, String planCode, String successUrl, String cancelUrl) {
        try {
            SessionCreateParams params = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
                    .setCustomer(customerId)
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .setClientReferenceId(String.valueOf(tenantId))
                    .putMetadata("tenantId", String.valueOf(tenantId))
                    .putMetadata("planCode", planCode)
                    .addLineItem(SessionCreateParams.LineItem.builder()
                            .setPrice(priceId)
                            .setQuantity(1L)
                            .build())
                    .build();
            Session session = Session.create(params);
            return new CheckoutSession(session.getId(), session.getUrl());
        } catch (StripeException e) {
            throw ApiException.unprocessable("STRIPE_ERROR", "Impossible de créer la session de paiement Stripe");
        }
    }

    @Override
    public Event parseWebhookEvent(String payload, String signatureHeader) {
        try {
            return Webhook.constructEvent(payload, signatureHeader, properties.webhookSecret());
        } catch (SignatureVerificationException e) {
            throw ApiException.badRequest("INVALID_STRIPE_SIGNATURE", "Signature Stripe invalide", List.of());
        }
    }
}
