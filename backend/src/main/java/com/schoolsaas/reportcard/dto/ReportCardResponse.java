package com.schoolsaas.reportcard.dto;

import com.schoolsaas.reportcard.ReportCard;
import java.time.LocalDate;
import java.util.List;

public record ReportCardResponse(
        Long id,
        Long studentId,
        Long schoolClassId,
        String periodLabel,
        LocalDate periodFrom,
        LocalDate periodTo,
        Double generalAverage,
        String generalComment,
        String councilDecision,
        int absenceCount,
        int lateCount,
        List<ReportCardEntryResponse> entries) {

    public static ReportCardResponse from(ReportCard reportCard, List<ReportCardEntryResponse> entries) {
        return new ReportCardResponse(
                reportCard.getId(),
                reportCard.getStudentId(),
                reportCard.getSchoolClassId(),
                reportCard.getPeriodLabel(),
                reportCard.getPeriodFrom(),
                reportCard.getPeriodTo(),
                reportCard.getGeneralAverage(),
                reportCard.getGeneralComment(),
                reportCard.getCouncilDecision(),
                reportCard.getAbsenceCount(),
                reportCard.getLateCount(),
                entries);
    }
}
