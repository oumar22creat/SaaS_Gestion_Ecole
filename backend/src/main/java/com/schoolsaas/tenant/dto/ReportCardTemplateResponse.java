package com.schoolsaas.tenant.dto;

import com.schoolsaas.tenant.Tenant;

public record ReportCardTemplateResponse(String reportCardHeader, String reportCardLegalMentions) {

    public static ReportCardTemplateResponse from(Tenant tenant) {
        return new ReportCardTemplateResponse(tenant.getReportCardHeader(), tenant.getReportCardLegalMentions());
    }
}
