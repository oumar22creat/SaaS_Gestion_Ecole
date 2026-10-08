package com.schoolsaas.reportcard.dto;

import com.schoolsaas.reportcard.ReportCardEntry;

/**
 * Une ligne de bulletin, détaillée comme le document l'imprime : les devoirs et la
 * composition à côté de la moyenne, et non la seule moyenne. Une famille qui voit 12 de
 * moyenne ne sait pas si l'élève a progressé entre le devoir et la composition ; avec le
 * détail, elle le sait.
 */
public record ReportCardEntryResponse(
        Long subjectId,
        Double assignmentOneScore,
        Double assignmentTwoScore,
        Double examScore,
        Double average,
        int coefficient,
        String teacherComment) {

    public static ReportCardEntryResponse from(ReportCardEntry entry) {
        return new ReportCardEntryResponse(
                entry.getSubjectId(),
                entry.getAssignmentOneScore(),
                entry.getAssignmentTwoScore(),
                entry.getExamScore(),
                entry.getAverage(),
                entry.getCoefficient(),
                entry.getTeacherComment());
    }
}
