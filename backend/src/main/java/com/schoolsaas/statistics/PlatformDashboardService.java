package com.schoolsaas.statistics;

import com.schoolsaas.billing.Plan;
import com.schoolsaas.billing.PlanRepository;
import com.schoolsaas.billing.Subscription;
import com.schoolsaas.billing.SubscriptionRepository;
import com.schoolsaas.billing.SubscriptionStatus;
import com.schoolsaas.common.NumberUtils;
import com.schoolsaas.notification.NotificationLogRepository;
import com.schoolsaas.statistics.dto.PlatformDashboardSummaryResponse;
import com.schoolsaas.tenant.TenantRepository;
import com.schoolsaas.tenant.TenantStatus;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Tableau de bord Super-Admin — cahier-des-charges.md §5/§18, ROADMAP.md 2.6/3.2. Vue
 * plateforme (tous établissements), pas de vue par tenant : voir {@link DashboardService}
 * pour le tableau de bord établissement.
 */
@Service
public class PlatformDashboardService {

    private final TenantRepository tenantRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final PlanRepository planRepository;
    private final NotificationLogRepository notificationLogRepository;

    public PlatformDashboardService(
            TenantRepository tenantRepository,
            SubscriptionRepository subscriptionRepository,
            PlanRepository planRepository,
            NotificationLogRepository notificationLogRepository) {
        this.tenantRepository = tenantRepository;
        this.subscriptionRepository = subscriptionRepository;
        this.planRepository = planRepository;
        this.notificationLogRepository = notificationLogRepository;
    }

    public PlatformDashboardSummaryResponse summary() {
        long trialCount = tenantRepository.countByStatus(TenantStatus.TRIAL);
        long activeCount = tenantRepository.countByStatus(TenantStatus.ACTIVE);
        long readOnlyCount = tenantRepository.countByStatus(TenantStatus.READ_ONLY);
        long suspendedCount = tenantRepository.countByStatus(TenantStatus.SUSPENDED);
        long cancelledCount = tenantRepository.countByStatus(TenantStatus.CANCELLED);

        List<Subscription> activeSubscriptions = subscriptionRepository.findAllByStatus(SubscriptionStatus.ACTIVE);
        Map<Long, Plan> plansById = planRepository.findAllById(activeSubscriptions.stream().map(Subscription::getPlanId).distinct().toList())
                .stream()
                .collect(Collectors.toMap(Plan::getId, plan -> plan));
        long mrrCents = activeSubscriptions.stream()
                .map(subscription -> plansById.get(subscription.getPlanId()))
                .filter(java.util.Objects::nonNull)
                .mapToLong(Plan::getPriceCents)
                .sum();
        String currency = plansById.values().stream().map(Plan::getCurrency).findFirst().orElse(null);

        long totalSubscriptions = subscriptionRepository.count();
        long cancelledSubscriptions = subscriptionRepository.countByStatus(SubscriptionStatus.CANCELED);
        long payingSubscriptions = subscriptionRepository.countByStatus(SubscriptionStatus.ACTIVE);

        long notificationsSentCount = notificationLogRepository.count();

        return new PlatformDashboardSummaryResponse(
                trialCount, activeCount, readOnlyCount, suspendedCount, cancelledCount,
                mrrCents, mrrCents * 12, currency,
                rate(cancelledSubscriptions, totalSubscriptions),
                rate(payingSubscriptions, totalSubscriptions),
                notificationsSentCount);
    }

    private static Double rate(long numerator, long denominator) {
        return denominator == 0 ? null : NumberUtils.round2(100.0 * numerator / denominator);
    }
}
