package com.schoolsaas.grade.dto;

public record ClassSubjectAverageResponse(Long schoolClassId, Long subjectId, Double average, int studentCount) {
}
