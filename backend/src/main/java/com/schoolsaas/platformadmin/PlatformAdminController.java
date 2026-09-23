package com.schoolsaas.platformadmin;

import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.platformadmin.dto.CashPaymentRequest;
import com.schoolsaas.platformadmin.dto.PlanAdminResponse;
import com.schoolsaas.platformadmin.dto.PlanChangeRequest;
import com.schoolsaas.platformadmin.dto.PlatformAdminCreateRequest;
import com.schoolsaas.platformadmin.dto.PlatformAdminResponse;
import com.schoolsaas.platformadmin.dto.TenantAdminResponse;
import com.schoolsaas.platformadmin.dto.TenantStatusUpdateRequest;
import com.schoolsaas.platformadmin.dto.TrialExtensionRequest;
import com.schoolsaas.tenant.TenantStatus;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Console plateforme (cahier-des-charges.md §5). Réservée au Super-Administrateur, qui gère
 * les établissements clients sans jamais accéder à leurs données scolaires (ADR-001).
 */
@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('SUPER_ADMIN')")
public class PlatformAdminController {

    private final PlatformAdminService platformAdminService;

    public PlatformAdminController(PlatformAdminService platformAdminService) {
        this.platformAdminService = platformAdminService;
    }

    @GetMapping("/tenants")
    public ApiResponse<List<TenantAdminResponse>> listTenants(
            Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) TenantStatus status) {
        Page<TenantAdminResponse> page = platformAdminService.listTenants(pageable, search, status);
        return ApiResponse.of(
                page.getContent(),
                new ApiResponse.PageMeta(page.getNumber() + 1, page.getSize(), page.getTotalElements()));
    }

    @GetMapping("/tenants/{id}")
    public ApiResponse<TenantAdminResponse> getTenant(@PathVariable Long id) {
        return ApiResponse.of(platformAdminService.getTenant(id));
    }

    /** Suspension pour impayé, réactivation après régularisation. Motif exigé si l'accès se restreint. */
    @PutMapping("/tenants/{id}/status")
    public ApiResponse<TenantAdminResponse> updateStatus(
            @PathVariable Long id, @Valid @RequestBody TenantStatusUpdateRequest request) {
        return ApiResponse.of(platformAdminService.updateStatus(id, request));
    }

    @PutMapping("/tenants/{id}/trial")
    public ApiResponse<TenantAdminResponse> extendTrial(
            @PathVariable Long id, @Valid @RequestBody TrialExtensionRequest request) {
        return ApiResponse.of(platformAdminService.extendTrial(id, request));
    }

    /**
     * Règlement encaissé en espèces : rouvre l'accès et repousse l'échéance. POST et non PUT —
     * chaque appel encaisse un règlement de plus, le rejouer n'est pas anodin.
     */
    @PostMapping("/tenants/{id}/payments")
    public ApiResponse<TenantAdminResponse> recordCashPayment(
            @PathVariable Long id, @Valid @RequestBody CashPaymentRequest request) {
        return ApiResponse.of(platformAdminService.recordCashPayment(id, request));
    }

    @PutMapping("/tenants/{id}/plan")
    public ApiResponse<TenantAdminResponse> changePlan(
            @PathVariable Long id, @Valid @RequestBody PlanChangeRequest request) {
        return ApiResponse.of(platformAdminService.changePlan(id, request));
    }

    /** Historique des décisions prises sur un établissement : ce qu'on relit avant de le rappeler. */
    @GetMapping("/tenants/{id}/actions")
    public ApiResponse<List<PlatformActionResponse>> tenantActions(@PathVariable Long id) {
        List<PlatformAdminAction> actions = platformAdminService.actionsForTenant(id);
        Map<Long, String> names = platformAdminService.tenantNamesOf(actions);
        return ApiResponse.of(actions.stream()
                .map(action -> PlatformActionResponse.from(action, names.get(action.getTenantId())))
                .toList());
    }

    @GetMapping("/plans")
    public ApiResponse<List<PlanAdminResponse>> listPlans() {
        return ApiResponse.of(platformAdminService.listPlans());
    }

    @GetMapping("/accounts")
    public ApiResponse<List<PlatformAdminResponse>> listAdmins() {
        return ApiResponse.of(platformAdminService.listAdmins().stream()
                .map(PlatformAdminResponse::from)
                .toList());
    }

    @PostMapping("/accounts")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<PlatformAdminResponse> createAdmin(@Valid @RequestBody PlatformAdminCreateRequest request) {
        return ApiResponse.of(PlatformAdminResponse.from(platformAdminService.createAdmin(request)));
    }

    /** Journal complet, toutes décisions confondues. */
    @GetMapping("/actions")
    public ApiResponse<List<PlatformActionResponse>> listActions(Pageable pageable) {
        Page<PlatformAdminAction> page = platformAdminService.listActions(pageable);
        Map<Long, String> names = platformAdminService.tenantNamesOf(page.getContent());
        return ApiResponse.of(
                page.map(action -> PlatformActionResponse.from(action, names.get(action.getTenantId()))).getContent(),
                new ApiResponse.PageMeta(page.getNumber() + 1, page.getSize(), page.getTotalElements()));
    }

    public record PlatformActionResponse(
            Long id, Long tenantId, String tenantName, String action, String detail, String reason,
            Long platformAdminId, java.time.Instant createdAt) {

        static PlatformActionResponse from(PlatformAdminAction action, String tenantName) {
            return new PlatformActionResponse(
                    action.getId(), action.getTenantId(), tenantName, action.getAction(), action.getDetail(),
                    action.getReason(), action.getPlatformAdminId(), action.getCreatedAt());
        }
    }
}
