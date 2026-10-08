package com.schoolsaas.termreport;

import com.schoolsaas.common.ApiException;
import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.tenant.Tenant;
import com.schoolsaas.tenant.TenantContext;
import com.schoolsaas.tenant.TenantLogoService;
import com.schoolsaas.tenant.TenantRepository;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Rapport trimestriel d'une classe, destiné à la tutelle.
 *
 * <p>Réservé à la direction et à l'administration : ce document engage l'établissement
 * auprès du ministère, il n'est pas de la main d'un enseignant ni du secrétariat.
 */
@RestController
@RequestMapping("/api/v1/term-reports")
@PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION')")
public class TermReportController {

    private final TermReportService termReportService;
    private final TermReportPdfExporter pdfExporter;
    private final TenantRepository tenantRepository;
    private final TenantLogoService tenantLogoService;

    public TermReportController(
            TermReportService termReportService,
            TermReportPdfExporter pdfExporter,
            TenantRepository tenantRepository,
            TenantLogoService tenantLogoService) {
        this.termReportService = termReportService;
        this.pdfExporter = pdfExporter;
        this.tenantRepository = tenantRepository;
        this.tenantLogoService = tenantLogoService;
    }

    /** Les chiffres seuls, pour les afficher à l'écran avant de produire le document. */
    @GetMapping("/classes/{schoolClassId}")
    public ApiResponse<TermReport> preview(
            @PathVariable Long schoolClassId, @RequestParam String periodLabel) {
        return ApiResponse.of(termReportService.build(schoolClassId, periodLabel));
    }

    @GetMapping("/classes/{schoolClassId}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable Long schoolClassId, @RequestParam String periodLabel) {
        TermReport rapport = termReportService.build(schoolClassId, periodLabel);
        Tenant tenant = tenantRepository.findById(TenantContext.get())
                .orElseThrow(() -> ApiException.notFound("TENANT_NOT_FOUND", "Établissement introuvable"));

        String nomFichier =
                "rapport-" + assainir(rapport.className()) + "-" + assainir(rapport.periodLabel()) + ".pdf";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(nomFichier).build().toString())
                .body(pdfExporter.export(rapport, tenant, tenantLogoService.content(tenant).orElse(null)));
    }

    private static String assainir(String valeur) {
        return java.text.Normalizer.normalize(valeur, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(java.util.Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");
    }
}
