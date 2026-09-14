package com.schoolsaas.billing;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    Optional<Subscription> findByTenantId(Long tenantId);

    Optional<Subscription> findByStripeSubscriptionId(String stripeSubscriptionId);

    /** Essais jamais convertis en paiement, dont la date de fin est dépassée depuis {@code threshold}. */
    List<Subscription> findAllByStatusAndStripeSubscriptionIdIsNullAndTrialEndsAtBefore(
            SubscriptionStatus status, Instant threshold);

    /** Abonnements en échec de paiement depuis {@code threshold} (voir docs/ARCHITECTURE.md ADR-009). */
    List<Subscription> findAllByStatusAndPaymentFailedAtBefore(SubscriptionStatus status, Instant threshold);

    long countByStatus(SubscriptionStatus status);

    List<Subscription> findAllByStatus(SubscriptionStatus status);
}
