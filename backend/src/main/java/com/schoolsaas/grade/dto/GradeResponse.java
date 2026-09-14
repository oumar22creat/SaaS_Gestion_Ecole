package com.schoolsaas.grade.dto;

import com.schoolsaas.grade.Grade;

public record GradeResponse(Long id, Long examId, Long studentId, Double score, boolean absent, String comment) {

    public static GradeResponse from(Grade grade) {
        return new GradeResponse(grade.getId(), grade.getExamId(), grade.getStudentId(), grade.getScore(), grade.isAbsent(), grade.getComment());
    }
}
