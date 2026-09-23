package com.schoolsaas.billing;

import com.schoolsaas.tenant.TenantRepository;
import com.schoolsaas.tenant.TenantStatus;
import java.time.Duration;
import java.time.Instant;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Blocage progressif de l'accès (cahier-des-charges.md §4.2 : "lecture seule, puis
 * blocage") — tourne périodiquement plutôt que sur un webhook Stripe, car le premier
 * déclencheur (fin d'essai sans carte) n'a jamais d'événement Stripe correspondant.
 *
 * <p>Chaque étape ({@code TRIAL}/{@code ACTIVE} → {@code READ_ONLY} → {@code SUSPENDED}) ne
 * s'applique que si le tenant est encore dans l'état précédent attendu : un tenant très en
 * retard (ex. job jamais exécuté depuis longtemps) ne descend donc que d'un cran par
 * exécution, pas directement à {@code SUSPENDED} — rattrapé sur l'exécution suivante, sans
 * impact en cadence normale (horaire).
 *
 * <p>Durées de grâce ({@code app.billing.past-due-grace-days}/{@code read-only-grace-days})
 * choisies comme point de départ raisonnable, à confirmer avec le porteur de projet — voir
 * docs/ARCHITECTURE.md ADR-009.
 */
@Component
public class TenantAccessLifecycleJob {

    private final SubscriptionRepository subscriptionRepository;
    private final TenantRepository tenantRepository;
    private final BillingProperties billingProperties;

    public TenantAccessLifecycleJob(
            SubscriptionRepository subscriptionRepository,
            TenantRepository tenantRepository,
            BillingProperties billingProperties) {
        this.subscriptionRepository = subscriptionRepository;
        this.tenantRepository = tenantRepository;
        this.billingProperties = billingProperties;
    }

    @Scheduled(cron = "0 0 * * * *")
    public void run() {
        evaluate(Instant.now());
    }

    @Transactional
    public void evaluate(Instant now) {
        downgradeExpiredTrials(now);
        downgradePastDueSubscriptions(now);
        expireCashSubscriptions(now);
    }

    /**
     * Ferme l'accès d'un établissement dont la période réglée en espèces est échue.
     *
     * <p>Contrairement aux deux étapes précédentes, celle-ci ne passe pas par
     * {@code READ_ONLY} : un règlement en espèces n'a pas de relance automatique, personne
     * n'avertit l'établissement pendant la lecture seule, et il découvrirait la coupure une
     * semaine après l'échéance sans comprendre pourquoi. Bloquer au jour dit avec un message
     * qui dit quoi faire est plus honnête — c'est le Super-Administrateur qui rouvrira
     * l'accès en enregistrant le règlement suivant.
     *
     * <p>L'abonnement passe à {@code EXPIRED} en même temps que le tenant à
     * {@code SUSPENDED} : sans cela la console plateforme afficherait un abonnement
     * « actif » en face d'un établissement coupé.
     */
    private void expireCashSubscriptions(Instant now) {
        Instant threshold = now.minus(Duration.ofDays(billingProperties.cashGraceDays()));

        subscriptionRepository
                .findAllByStatusAndStripeSubscriptionIdIsNullAndCurrentPeriodEndBefore(SubscriptionStatus.ACTIVE, threshold)
                .forEach(subscription -> {
                    subscription.setStatus(SubscriptionStatus.EXPIRED);
                    subscriptionRepository.save(subscription);
                    // Un tenant déjà CANCELLED (résiliation décidée depuis la console) n'est pas
                    // ramené à SUSPENDED : ce serait laisser croire qu'un règlement suffirait à
                    // le rouvrir.
                    setTenantStatusIfCurrently(subscription.getTenantId(), TenantStatus.ACTIVE, TenantStatus.SUSPENDED);
                    setTenantStatusIfCurrently(subscription.getTenantId(), TenantStatus.READ_ONLY, TenantStatus.SUSPENDED);
                });
    }

    private void downgradeExpiredTrials(Instant now) {
        Instant suspendThreshold = now.minus(Duration.ofDays(billingProperties.readOnlyGraceDays()));

        subscriptionRepository
                .findAllByStatusAndStripeSubscriptionIdIsNullAndTrialEndsAtBefore(SubscriptionStatus.TRIALING, suspendThreshold)
                .forEach(sub -> setTenantStatusIfCurrently(sub.getTenantId(), TenantStatus.READ_ONLY, TenantStatus.SUSPENDED));

        subscriptionRepository
                .findAllByStatusAndStripeSubscriptionIdIsNullAndTrialEndsAtBefore(SubscriptionStatus.TRIALING, now)
                .forEach(sub -> setTenantStatusIfCurrently(sub.getTenantId(), TenantStatus.TRIAL, TenantStatus.READ_ONLY));
    }

    private void downgradePastDueSubscriptions(Instant now) {
        Instant readOnlyThreshold = now.minus(Duration.ofDays(billingProperties.pastDueGraceDays()));
        Instant suspendThreshold = readOnlyThreshold.minus(Duration.ofDays(billingProperties.readOnlyGraceDays()));

        subscriptionRepository.findAllByStatusAndPaymentFailedAtBefore(SubscriptionStatus.PAST_DUE, suspendThreshold)
                .forEach(sub -> setTenantStatusIfCurrently(sub.getTenantId(), TenantStatus.READ_ONLY, TenantStatus.SUSPENDED));

        subscriptionRepository.findAllByStatusAndPaymentFailedAtBefore(SubscriptionStatus.PAST_DUE, readOnlyThreshold)
                .forEach(sub -> setTenantStatusIfCurrently(sub.getTenantId(), TenantStatus.ACTIVE, TenantStatus.READ_ONLY));
    }

    private void setTenantStatusIfCurrently(Long tenantId, TenantStatus expectedCurrent, TenantStatus newStatus) {
        tenantRepository.findById(tenantId)
                .filter(tenant -> tenant.getStatus() == expectedCurrent)
                .ifPresent(tenant -> {
                    tenant.setStatus(newStatus);
                    tenantRepository.save(tenant);
                });
    }
}
