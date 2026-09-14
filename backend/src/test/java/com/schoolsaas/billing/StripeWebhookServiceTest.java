package com.schoolsaas.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.schoolsaas.common.ApiException;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantRepository;
import com.schoolsaas.tenant.TenantStatus;
import com.stripe.Stripe;
import com.stripe.model.Event;
import com.stripe.net.ApiResource;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test unitaire (JUnit + Mockito, voir CLAUDE.md règle 4) — seule {@link StripeGateway} est
 * mockée (pas d'appel réseau Stripe réel). Les événements Stripe sont de VRAIS objets
 * {@link Event}, désérialisés depuis du JSON via {@code ApiResource.GSON} — exactement le
 * mécanisme interne utilisé par le SDK (voir {@code Webhook.constructEvent}) : mocker
 * directement {@code Event}/{@code EventDataObjectDeserializer} avec Mockito échoue sur cet
 * environnement (classes du SDK non modifiables par le mock maker inline, voir l'échec
 * observé lors de l'exécution des tests), et cette approche teste en plus la vraie
 * désérialisation plutôt qu'une simulation.
 */
@ExtendWith(MockitoExtension.class)
class StripeWebhookServiceTest {

    @Mock
    private StripeGateway stripeGateway;

    @Mock
    private SubscriptionRepository subscriptionRepository;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private TenantRepository tenantRepository;

    private StripeWebhookService service;

    @BeforeEach
    void setUp() {
        service = new StripeWebhookService(stripeGateway, subscriptionRepository, invoiceRepository, tenantRepository);
    }

    @Test
    void checkoutSessionCompletedActivatesSubscriptionAndTenant() {
        Long tenantId = 42L;
        Event event = eventFrom(
                "checkout.session.completed",
                "{\"id\":\"cs_test_123\",\"object\":\"checkout.session\","
                        + "\"client_reference_id\":\"" + tenantId + "\","
                        + "\"customer\":\"cus_123\",\"subscription\":\"sub_123\"}");
        when(stripeGateway.parseWebhookEvent("payload", "sig")).thenReturn(event);

        Subscription subscription = new Subscription(tenantId, 1L, SubscriptionStatus.TRIALING, Instant.now());
        when(subscriptionRepository.findByTenantId(tenantId)).thenReturn(Optional.of(subscription));
        Tenant tenant = new Tenant("École", "ecole-checkout", TenantStatus.TRIAL);
        when(tenantRepository.findById(tenantId)).thenReturn(Optional.of(tenant));

        service.handle("payload", "sig");

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getStripeCustomerId()).isEqualTo("cus_123");
        assertThat(subscription.getStripeSubscriptionId()).isEqualTo("sub_123");
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.ACTIVE);
    }

    @Test
    void invoicePaymentSucceededMarksInvoicePaidAndReactivatesTenant() {
        Subscription subscription = new Subscription(7L, 1L, SubscriptionStatus.PAST_DUE, Instant.now());
        subscription.setStripeSubscriptionId("sub_456");
        subscription.setPaymentFailedAt(Instant.now().minusSeconds(3600));
        when(subscriptionRepository.findByStripeSubscriptionId("sub_456")).thenReturn(Optional.of(subscription));
        Tenant tenant = new Tenant("École", "ecole-invoice-ok", TenantStatus.READ_ONLY);
        when(tenantRepository.findById(7L)).thenReturn(Optional.of(tenant));
        when(invoiceRepository.findByStripeInvoiceId("in_ok")).thenReturn(Optional.empty());

        Event event = eventFrom("invoice.payment_succeeded", invoiceJsonLinkedTo("sub_456", "in_ok"));
        when(stripeGateway.parseWebhookEvent("payload", "sig")).thenReturn(event);

        service.handle("payload", "sig");

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(subscription.getPaymentFailedAt()).isNull();
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.ACTIVE);
    }

    @Test
    void invoicePaymentFailedMarksSubscriptionPastDueWithoutTouchingTenantImmediately() {
        Subscription subscription = new Subscription(9L, 1L, SubscriptionStatus.ACTIVE, Instant.now());
        subscription.setStripeSubscriptionId("sub_789");
        when(subscriptionRepository.findByStripeSubscriptionId("sub_789")).thenReturn(Optional.of(subscription));
        when(invoiceRepository.findByStripeInvoiceId("in_fail")).thenReturn(Optional.empty());

        Event event = eventFrom("invoice.payment_failed", invoiceJsonLinkedTo("sub_789", "in_fail"));
        when(stripeGateway.parseWebhookEvent("payload", "sig")).thenReturn(event);

        service.handle("payload", "sig");

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
        assertThat(subscription.getPaymentFailedAt()).isNotNull();
        // Le blocage progressif proprement dit est du ressort de TenantAccessLifecycleJob
        // (grâce de plusieurs jours), pas de ce webhook — voir docs/ARCHITECTURE.md ADR-009.
        org.mockito.Mockito.verifyNoInteractions(tenantRepository);
    }

    @Test
    void subscriptionDeletedCancelsSubscriptionAndTenant() {
        Subscription subscription = new Subscription(13L, 1L, SubscriptionStatus.ACTIVE, Instant.now());
        subscription.setStripeSubscriptionId("sub_del");
        when(subscriptionRepository.findByStripeSubscriptionId("sub_del")).thenReturn(Optional.of(subscription));
        Tenant tenant = new Tenant("École", "ecole-cancel", TenantStatus.ACTIVE);
        when(tenantRepository.findById(13L)).thenReturn(Optional.of(tenant));

        Event event = eventFrom("customer.subscription.deleted", "{\"id\":\"sub_del\",\"object\":\"subscription\"}");
        when(stripeGateway.parseWebhookEvent("payload", "sig")).thenReturn(event);

        service.handle("payload", "sig");

        assertThat(subscription.getStatus()).isEqualTo(SubscriptionStatus.CANCELED);
        assertThat(tenant.getStatus()).isEqualTo(TenantStatus.CANCELLED);
    }

    @Test
    void invalidSignaturePropagatesAsApiException() {
        when(stripeGateway.parseWebhookEvent(any(), any()))
                .thenThrow(ApiException.badRequest("INVALID_STRIPE_SIGNATURE", "Signature Stripe invalide", java.util.List.of()));

        assertThatThrownBy(() -> service.handle("payload", "bad-sig")).isInstanceOf(ApiException.class);
    }

    private String invoiceJsonLinkedTo(String stripeSubscriptionId, String invoiceId) {
        return "{\"id\":\"" + invoiceId + "\",\"object\":\"invoice\","
                + "\"amount_due\":4900,\"currency\":\"xof\","
                + "\"period_start\":1700000000,\"period_end\":1702600000,"
                + "\"hosted_invoice_url\":\"https://stripe.test/" + invoiceId + "\","
                + "\"parent\":{\"type\":\"subscription_details\","
                + "\"subscription_details\":{\"subscription\":\"" + stripeSubscriptionId + "\"}}}";
    }

    /** Construit un {@link Event} réel via la désérialisation Gson du SDK — voir la javadoc de classe. */
    private Event eventFrom(String type, String dataObjectJson) {
        String json = "{\"id\":\"evt_test\",\"object\":\"event\",\"api_version\":\"" + Stripe.API_VERSION + "\","
                + "\"type\":\"" + type + "\",\"data\":{\"object\":" + dataObjectJson + "}}";
        return ApiResource.GSON.fromJson(json, Event.class);
    }
}
