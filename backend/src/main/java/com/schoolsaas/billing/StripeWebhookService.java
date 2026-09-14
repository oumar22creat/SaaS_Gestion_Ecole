package com.schoolsaas.billing;

import com.schoolsaas.common.ApiException;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantRepository;
import com.schoolsaas.tenant.TenantStatus;
import com.stripe.model.Event;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Synchronise le statut de l'abonnement (et l'accès du tenant) depuis les événements Stripe
 * (cahier-des-charges.md §4.3). Traitement volontairement idempotent (upsert par
 * {@code stripe_invoice_id}/{@code stripe_subscription_id}) plutôt qu'une table d'événements
 * déjà traités : Stripe peut redélivrer un même événement, et rejouer ces traitements est
 * sans effet de bord (pas de compteur incrémenté, uniquement des mises à jour d'état).
 */
@Service
public class StripeWebhookService {

    private final StripeGateway stripeGateway;
    private final SubscriptionRepository subscriptionRepository;
    private final InvoiceRepository invoiceRepository;
    private final TenantRepository tenantRepository;

    public StripeWebhookService(
            StripeGateway stripeGateway,
            SubscriptionRepository subscriptionRepository,
            InvoiceRepository invoiceRepository,
            TenantRepository tenantRepository) {
        this.stripeGateway = stripeGateway;
        this.subscriptionRepository = subscriptionRepository;
        this.invoiceRepository = invoiceRepository;
        this.tenantRepository = tenantRepository;
    }

    @Transactional
    public void handle(String payload, String signatureHeader) {
        Event event = stripeGateway.parseWebhookEvent(payload, signatureHeader);
        switch (event.getType()) {
            case "checkout.session.completed" -> handleCheckoutCompleted(event);
            case "invoice.payment_succeeded" -> handleInvoiceEvent(event, InvoiceStatus.PAID);
            case "invoice.payment_failed" -> handleInvoiceEvent(event, InvoiceStatus.OPEN);
            case "customer.subscription.deleted" -> handleSubscriptionDeleted(event);
            default -> {
                // Événement Stripe non pertinent pour ce module — ignoré volontairement.
            }
        }
    }

    private void handleCheckoutCompleted(Event event) {
        Session session = deserialize(event, Session.class);
        Long tenantId = Long.parseLong(session.getClientReferenceId());
        Subscription subscription = subscriptionRepository.findByTenantId(tenantId)
                .orElseThrow(() -> ApiException.notFound("SUBSCRIPTION_NOT_FOUND", "Abonnement introuvable"));

        subscription.setStripeCustomerId(session.getCustomer());
        subscription.setStripeSubscriptionId(session.getSubscription());
        subscription.setStatus(SubscriptionStatus.ACTIVE);
        subscription.setPaymentFailedAt(null);
        subscriptionRepository.save(subscription);

        activateTenant(tenantId);
    }

    private void handleInvoiceEvent(Event event, InvoiceStatus status) {
        com.stripe.model.Invoice stripeInvoice = deserialize(event, com.stripe.model.Invoice.class);
        String stripeSubscriptionId = extractSubscriptionId(stripeInvoice);
        if (stripeSubscriptionId == null) {
            return; // facture hors abonnement (ex. ajustement manuel) — hors périmètre MVP
        }
        Subscription subscription = subscriptionRepository.findByStripeSubscriptionId(stripeSubscriptionId)
                .orElseThrow(() -> ApiException.notFound("SUBSCRIPTION_NOT_FOUND", "Abonnement introuvable"));

        upsertInvoice(subscription, stripeInvoice, status);

        if (status == InvoiceStatus.PAID) {
            subscription.setStatus(SubscriptionStatus.ACTIVE);
            subscription.setPaymentFailedAt(null);
            if (stripeInvoice.getPeriodEnd() != null) {
                subscription.setCurrentPeriodEnd(Instant.ofEpochSecond(stripeInvoice.getPeriodEnd()));
            }
            subscriptionRepository.save(subscription);
            activateTenant(subscription.getTenantId());
        } else {
            subscription.setStatus(SubscriptionStatus.PAST_DUE);
            if (subscription.getPaymentFailedAt() == null) {
                subscription.setPaymentFailedAt(Instant.now());
            }
            subscriptionRepository.save(subscription);
        }
    }

    private void upsertInvoice(Subscription subscription, com.stripe.model.Invoice stripeInvoice, InvoiceStatus status) {
        Invoice invoice = invoiceRepository.findByStripeInvoiceId(stripeInvoice.getId())
                .orElseGet(() -> new Invoice(
                        subscription.getTenantId(),
                        subscription.getId(),
                        stripeInvoice.getId(),
                        stripeInvoice.getAmountDue() != null ? stripeInvoice.getAmountDue().intValue() : 0,
                        stripeInvoice.getCurrency(),
                        status,
                        stripeInvoice.getPeriodStart() != null ? Instant.ofEpochSecond(stripeInvoice.getPeriodStart()) : null,
                        stripeInvoice.getPeriodEnd() != null ? Instant.ofEpochSecond(stripeInvoice.getPeriodEnd()) : null,
                        stripeInvoice.getHostedInvoiceUrl()));
        invoice.setStatus(status);
        if (status == InvoiceStatus.PAID) {
            invoice.setPaidAt(Instant.now());
        }
        invoiceRepository.save(invoice);
    }

    private void handleSubscriptionDeleted(Event event) {
        com.stripe.model.Subscription stripeSubscription = deserialize(event, com.stripe.model.Subscription.class);
        subscriptionRepository.findByStripeSubscriptionId(stripeSubscription.getId()).ifPresent(subscription -> {
            subscription.setStatus(SubscriptionStatus.CANCELED);
            subscriptionRepository.save(subscription);

            Tenant tenant = tenantRepository.findById(subscription.getTenantId())
                    .orElseThrow(() -> ApiException.notFound("TENANT_NOT_FOUND", "Établissement introuvable"));
            tenant.setStatus(TenantStatus.CANCELLED);
            tenantRepository.save(tenant);
        });
    }

    private void activateTenant(Long tenantId) {
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> ApiException.notFound("TENANT_NOT_FOUND", "Établissement introuvable"));
        tenant.setStatus(TenantStatus.ACTIVE);
        tenantRepository.save(tenant);
    }

    private static String extractSubscriptionId(com.stripe.model.Invoice invoice) {
        if (invoice.getParent() == null || invoice.getParent().getSubscriptionDetails() == null) {
            return null;
        }
        return invoice.getParent().getSubscriptionDetails().getSubscription();
    }

    private <T extends StripeObject> T deserialize(Event event, Class<T> type) {
        StripeObject stripeObject = event.getDataObjectDeserializer().getObject()
                .orElseThrow(() -> ApiException.unprocessable(
                        "STRIPE_EVENT_UNREADABLE", "Impossible de lire l'événement Stripe"));
        return type.cast(stripeObject);
    }
}
