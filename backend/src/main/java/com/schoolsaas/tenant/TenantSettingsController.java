package com.schoolsaas.tenant;

import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.tenant.dto.CustomDomainResponse;
import com.schoolsaas.tenant.dto.CustomDomainUpdateRequest;
import com.schoolsaas.tenant.dto.ReportCardTemplateResponse;
import com.schoolsaas.tenant.dto.ReportCardTemplateUpdateRequest;
import com.schoolsaas.tenant.dto.TenantBrandingResponse;
import com.schoolsaas.tenant.dto.TenantBrandingUpdateRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Personnalisation par établissement — cahier-des-charges.md §2.3/§2.4, ROADMAP.md 3.7.
 * {@code GET /branding} est volontairement public (voir SecurityConfig) : Web et Mobile
 * l'appellent au démarrage, avant toute connexion, pour appliquer le branding du tenant résolu
 * par sous-domaine/en-tête (voir TenantResolver) — auparavant absent, voir ADR-015 (Phase 1) et
 * ADR-028 (résolu ici).
 */
@RestController
@RequestMapping("/api/v1/tenants/current")
public class TenantSettingsController {

    private final TenantSettingsService tenantSettingsService;
    private final TenantLogoService tenantLogoService;

    public TenantSettingsController(
            TenantSettingsService tenantSettingsService, TenantLogoService tenantLogoService) {
        this.tenantSettingsService = tenantSettingsService;
        this.tenantLogoService = tenantLogoService;
    }

    @GetMapping("/branding")
    public TenantBrandingResponse branding() {
        return TenantBrandingResponse.from(tenantSettingsService.currentTenant());
    }

    @PutMapping("/branding")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION')")
    public ApiResponse<TenantBrandingResponse> updateBranding(@Valid @RequestBody TenantBrandingUpdateRequest request) {
        return ApiResponse.of(TenantBrandingResponse.from(tenantSettingsService.updateBranding(request)));
    }

    @GetMapping("/report-card-template")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION')")
    public ApiResponse<ReportCardTemplateResponse> reportCardTemplate() {
        return ApiResponse.of(ReportCardTemplateResponse.from(tenantSettingsService.currentTenant()));
    }

    @PutMapping("/report-card-template")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION')")
    public ApiResponse<ReportCardTemplateResponse> updateReportCardTemplate(@RequestBody ReportCardTemplateUpdateRequest request) {
        return ApiResponse.of(ReportCardTemplateResponse.from(tenantSettingsService.updateReportCardTemplate(request)));
    }

    @GetMapping("/custom-domain")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION')")
    public ApiResponse<CustomDomainResponse> customDomain() {
        return ApiResponse.of(CustomDomainResponse.from(tenantSettingsService.currentTenant()));
    }

    @PutMapping("/custom-domain")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION')")
    public ApiResponse<CustomDomainResponse> updateCustomDomain(@Valid @RequestBody CustomDomainUpdateRequest request) {
        return ApiResponse.of(CustomDomainResponse.from(tenantSettingsService.updateCustomDomain(request)));
    }

    /**
     * Logo de l'établissement, utilisé sur les documents officiels (bulletin, certificat,
     * reçu). Téléversé et non repris de {@code logoUrl} : les PDF sont fabriqués par le
     * serveur, et suivre une adresse saisie par un administrateur en ferait un relais vers
     * n'importe quelle machine du réseau interne.
     */
    @PostMapping(value = "/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION')")
    public ApiResponse<TenantBrandingResponse> uploadLogo(@RequestParam("file") MultipartFile file) {
        return ApiResponse.of(TenantBrandingResponse.from(tenantLogoService.upload(file)));
    }

    @DeleteMapping("/logo")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION')")
    public ApiResponse<TenantBrandingResponse> removeLogo() {
        return ApiResponse.of(TenantBrandingResponse.from(tenantLogoService.remove()));
    }

    /**
     * Sert le logo téléversé. Public comme {@code GET /branding} : Web et Mobile l'affichent
     * avant toute connexion, sur l'écran d'accueil du sous-domaine.
     */
    @GetMapping("/logo")
    public ResponseEntity<byte[]> logo() {
        Tenant tenant = tenantSettingsService.currentTenant();
        return tenantLogoService.content(tenant)
                .map(content -> ResponseEntity.ok()
                        .contentType(contentTypeOf(content))
                        // Un logo change rarement ; sans cache, chaque écran le retélécharge.
                        .cacheControl(CacheControl.maxAge(Duration.ofHours(1)).cachePublic())
                        .body(content))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    /** Le type est déduit de la signature du fichier, la seule source fiable à la relecture. */
    private static MediaType contentTypeOf(byte[] content) {
        boolean png = content.length > 4 && (content[0] & 0xFF) == 0x89 && content[1] == 'P';
        return png ? MediaType.IMAGE_PNG : MediaType.IMAGE_JPEG;
    }
}
