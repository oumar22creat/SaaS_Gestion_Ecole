package com.schoolsaas.billing.dto;

import com.schoolsaas.billing.Plan;

public record PlanResponse(
        String code, String name, Integer priceCents, String currency, Integer maxStudents, boolean purchasable) {

    public static PlanResponse from(Plan plan) {
        return new PlanResponse(
                plan.getCode(),
                plan.getName(),
                plan.getPriceCents(),
                plan.getCurrency(),
                plan.getMaxStudents(),
                plan.getStripePriceId() != null);
    }
}
