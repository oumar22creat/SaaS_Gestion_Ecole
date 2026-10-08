package com.schoolsaas.tenant.dto;

import com.schoolsaas.tenant.Tenant;

public record ReportCardTemplateResponse(
        String reportCardHeader,
        String reportCardLegalMentions,
        String officialAuthority,
        String academyLabel,
        String inspectionLabel,
        String directorName,
        String headOfficeCity,
        String postalAddress) {

    public static ReportCardTemplateResponse from(Tenant tenant) {
        return new ReportCardTemplateResponse(
                tenant.getReportCardHeader(),
                tenant.getReportCardLegalMentions(),
                tenant.getOfficialAuthority(),
                tenant.getAcademyLabel(),
                tenant.getInspectionLabel(),
                tenant.getDirectorName(),
                tenant.getHeadOfficeCity(),
                tenant.getPostalAddress());
    }
}
