package com.schoolsaas.onboarding;

import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.onboarding.dto.OnboardingStatusResponse;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Avancement de la configuration initiale, affiché sur le tableau de bord (ROADMAP.md 1.3). */
@RestController
@RequestMapping("/api/v1/onboarding")
public class OnboardingController {

    private final OnboardingService onboardingService;

    public OnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'SECRETARY')")
    public ApiResponse<OnboardingStatusResponse> status() {
        return ApiResponse.of(onboardingService.status());
    }
}
