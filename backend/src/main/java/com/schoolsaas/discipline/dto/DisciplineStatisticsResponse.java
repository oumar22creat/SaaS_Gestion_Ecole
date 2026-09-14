package com.schoolsaas.discipline.dto;

import com.schoolsaas.discipline.IncidentSeverity;
import com.schoolsaas.discipline.SanctionType;
import java.time.LocalDate;
import java.util.Map;

/** Statistiques de vie scolaire (cahier-des-charges.md §17) sur une classe et une période données. */
public record DisciplineStatisticsResponse(
        Long schoolClassId,
        LocalDate periodFrom,
        LocalDate periodTo,
        long incidentCount,
        Map<IncidentSeverity, Long> incidentsBySeverity,
        Map<SanctionType, Long> sanctionsByType) {
}
