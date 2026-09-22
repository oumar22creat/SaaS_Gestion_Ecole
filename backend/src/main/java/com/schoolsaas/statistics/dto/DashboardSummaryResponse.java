package com.schoolsaas.statistics.dto;

import com.schoolsaas.schoolfees.dto.FeeSummaryResponse;
import java.time.LocalDate;

/**
 * Statistiques de base de l'établissement — cahier-des-charges.md §18, ROADMAP.md 1.9.
 *
 * <p>{@code finance} est nul tant qu'aucune grille tarifaire n'existe : un bloc comptable à
 * zéro sur un établissement qui ne facture pas encore se lirait comme un défaut de
 * recouvrement.
 */
public record DashboardSummaryResponse(
        long studentCount,
        long teacherCount,
        long classCount,
        LocalDate periodFrom,
        LocalDate periodTo,
        Double attendanceRate,
        Double averageGrade,
        FeeSummaryResponse finance) {
}
