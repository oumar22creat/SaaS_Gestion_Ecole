package com.schoolsaas.grade.dto;

/** Moyenne de classe, minimum, maximum pour une évaluation (cahier-des-charges.md §11). */
public record ExamStatisticsResponse(Long examId, int gradedCount, Double average, Double min, Double max) {
}
