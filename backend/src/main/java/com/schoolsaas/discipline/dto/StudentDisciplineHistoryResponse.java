package com.schoolsaas.discipline.dto;

import java.util.List;

/** Historique disciplinaire d'un élève (cahier-des-charges.md §17) : vue agrégée, pas une nouvelle entité. */
public record StudentDisciplineHistoryResponse(
        List<SanctionResponse> sanctions, List<ObservationResponse> observations, List<ConvocationResponse> convocations) {
}
