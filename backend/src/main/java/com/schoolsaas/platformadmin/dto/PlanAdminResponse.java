package com.schoolsaas.platformadmin.dto;

import com.schoolsaas.billing.Plan;

public record PlanAdminResponse(
        Long id,
        String code,
        String name,
        Integer priceCents,
        String currency,
        Integer maxStudents,
        long tenantCount) {

    public static PlanAdminResponse from(Plan plan, long tenantCount) {
        return new PlanAdminResponse(
                plan.getId(), plan.getCode(), plan.getName(), plan.getPriceCents(),
                plan.getCurrency(), plan.getMaxStudents(), tenantCount);
    }
}
