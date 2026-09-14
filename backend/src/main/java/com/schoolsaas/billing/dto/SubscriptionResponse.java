package com.schoolsaas.billing.dto;

import com.schoolsaas.billing.Plan;
import com.schoolsaas.billing.Subscription;
import java.time.Instant;

public record SubscriptionResponse(
        String status, String planCode, String planName, Instant trialEndsAt, Instant currentPeriodEnd) {

    public static SubscriptionResponse from(Subscription subscription, Plan plan) {
        return new SubscriptionResponse(
                subscription.getStatus().name(),
                plan.getCode(),
                plan.getName(),
                subscription.getTrialEndsAt(),
                subscription.getCurrentPeriodEnd());
    }
}
