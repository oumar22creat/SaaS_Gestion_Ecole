package com.schoolsaas.grade.dto;

import com.schoolsaas.grade.Exam;
import com.schoolsaas.grade.ExamType;
import java.time.LocalDate;

public record ExamResponse(
        Long id, Long schoolClassId, Long subjectId, String label, double maxScore, int coefficient, LocalDate examDate,
        ExamType examType) {

    public static ExamResponse from(Exam exam) {
        return new ExamResponse(
                exam.getId(), exam.getSchoolClassId(), exam.getSubjectId(), exam.getLabel(), exam.getMaxScore(),
                exam.getCoefficient(), exam.getExamDate(), exam.getExamType());
    }
}
