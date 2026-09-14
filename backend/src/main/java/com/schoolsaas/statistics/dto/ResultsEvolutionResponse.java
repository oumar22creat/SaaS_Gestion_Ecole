package com.schoolsaas.statistics.dto;

import java.util.List;

public record ResultsEvolutionResponse(Long schoolClassId, Long subjectId, List<GradeEvolutionPointResponse> points) {
}
