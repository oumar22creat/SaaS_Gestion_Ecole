package com.schoolsaas.statistics;

import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.statistics.dto.ResultsEvolutionResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Statistiques avancées — cahier-des-charges.md §18, ROADMAP.md 3.2. */
@RestController
@RequestMapping("/api/v1/statistics/advanced")
@PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION')")
public class AdvancedStatisticsController {

    private final AdvancedStatisticsService advancedStatisticsService;

    public AdvancedStatisticsController(AdvancedStatisticsService advancedStatisticsService) {
        this.advancedStatisticsService = advancedStatisticsService;
    }

    @GetMapping("/results-evolution")
    public ApiResponse<ResultsEvolutionResponse> resultsEvolution(
            @RequestParam Long schoolClassId, @RequestParam(required = false) Long subjectId) {
        return ApiResponse.of(advancedStatisticsService.resultsEvolution(schoolClassId, subjectId));
    }

    @GetMapping(value = "/results-evolution.csv", produces = "text/csv")
    public ResponseEntity<String> resultsEvolutionCsv(
            @RequestParam Long schoolClassId, @RequestParam(required = false) Long subjectId) {
        String csv = advancedStatisticsService.resultsEvolutionCsv(schoolClassId, subjectId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"resultats-evolution.csv\"")
                .body(csv);
    }
}
