package com.schoolsaas.tenant;

import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.tenant.dto.TenantRegistrationRequest;
import com.schoolsaas.tenant.dto.TenantRegistrationResponse;
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

    public TenantRegistrationController(TenantRegistrationService tenantRegistrationService) {
        this.tenantRegistrationService = tenantRegistrationService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TenantRegistrationResponse> register(@Valid @RequestBody TenantRegistrationRequest request) {
        return ApiResponse.of(tenantRegistrationService.register(request));
    }
}
