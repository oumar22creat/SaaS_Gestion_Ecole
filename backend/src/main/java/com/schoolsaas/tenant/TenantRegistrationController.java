package com.schoolsaas.tenant;

import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.tenant.dto.TenantRegistrationRequest;
import com.schoolsaas.tenant.dto.TenantRegistrationResponse;
import com.schoolsaas.common.ratelimit.ClientIpResolver;
import com.schoolsaas.common.ratelimit.RateLimitProperties;
import com.schoolsaas.common.ratelimit.RateLimiter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Inscription self-service d'un établissement — voir cahier-des-charges.md §20.1. */
@RestController
@RequestMapping("/api/v1/tenants")
public class TenantRegistrationController {

    private final TenantRegistrationService tenantRegistrationService;
    private final RateLimiter rateLimiter;
    private final RateLimitProperties rateLimitProperties;

    public TenantRegistrationController(
            TenantRegistrationService tenantRegistrationService,
            RateLimiter rateLimiter,
            RateLimitProperties rateLimitProperties) {
        this.tenantRegistrationService = tenantRegistrationService;
        this.rateLimiter = rateLimiter;
        this.rateLimitProperties = rateLimitProperties;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TenantRegistrationResponse> register(
            @Valid @RequestBody TenantRegistrationRequest request, HttpServletRequest httpRequest) {
        // Route publique : sans quota, une seule machine peut créer des établissements en
        // masse et polluer la plateforme (cahier-des-charges.md §20.1).
        rateLimiter.consume(
                "tenant-registration", ClientIpResolver.resolve(httpRequest), rateLimitProperties.registrationPerIp());
        return ApiResponse.of(tenantRegistrationService.register(request));
    }
}
