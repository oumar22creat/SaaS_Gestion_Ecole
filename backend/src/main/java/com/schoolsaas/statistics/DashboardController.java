package com.schoolsaas.statistics;

import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.statistics.dto.DashboardSummaryResponse;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Tableau de bord établissement — cahier-des-charges.md §18, ROADMAP.md 1.9. */
@RestController
@RequestMapping("/api/v1/dashboard")
@PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION')")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /** Taux de présence calculé sur (from, to) — 30 derniers jours par défaut si omis. */
    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryResponse> summary(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        LocalDate effectiveTo = to != null ? to : LocalDate.now();
        LocalDate effectiveFrom = from != null ? from : effectiveTo.minus(30, ChronoUnit.DAYS);
        return ApiResponse.of(dashboardService.summary(effectiveFrom, effectiveTo));
    }
}
