package com.schoolsaas.grade.dto;

/** Moyenne pondérée (par coefficient d'évaluation), normalisée sur 20. */
public record SubjectAverageResponse(Long subjectId, Double average, int examCount) {
}
