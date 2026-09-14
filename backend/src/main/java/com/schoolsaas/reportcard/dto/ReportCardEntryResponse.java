package com.schoolsaas.reportcard.dto;

import com.schoolsaas.reportcard.ReportCardEntry;

public record ReportCardEntryResponse(Long subjectId, Double average, int coefficient, String teacherComment) {

    public static ReportCardEntryResponse from(ReportCardEntry entry) {
        return new ReportCardEntryResponse(entry.getSubjectId(), entry.getAverage(), entry.getCoefficient(), entry.getTeacherComment());
    }
}
