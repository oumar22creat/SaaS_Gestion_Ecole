package com.schoolsaas.statistics;

import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.statistics.dto.PlatformDashboardSummaryResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Tableau de bord Super-Admin — cahier-des-charges.md §5/§18, ROADMAP.md 2.6. */
@RestController
@RequestMapping("/api/v1/admin/dashboard")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class PlatformDashboardController {

    private final PlatformDashboardService platformDashboardService;

    public PlatformDashboardController(PlatformDashboardService platformDashboardService) {
        this.platformDashboardService = platformDashboardService;
    }

    @GetMapping("/summary")
    public ApiResponse<PlatformDashboardSummaryResponse> summary() {
        return ApiResponse.of(platformDashboardService.summary());
    }
}
