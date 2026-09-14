package com.schoolsaas.statistics.dto;

import java.time.LocalDate;

/** Statistiques de base de l'établissement — cahier-des-charges.md §18, ROADMAP.md 1.9. */
public record DashboardSummaryResponse(
        long studentCount,
        long teacherCount,
        long classCount,
        LocalDate periodFrom,
        LocalDate periodTo,
        Double attendanceRate,
        Double averageGrade) {
}
