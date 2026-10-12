package com.schoolsaas.paperwork;

import com.schoolsaas.common.ApiException;
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
import org.springframework.web.bind.annotation.RestController;

/**
 * Documents officiels remis au guichet (cahier-des-charges.md §7/§19.4).
 *
 * <p>Le certificat de scolarité est délivré par le secrétariat ; le reçu de règlement par qui
 * encaisse. Les deux engagent l'établissement, d'où des rôles distincts et restreints.
 */
@RestController
@RequestMapping("/api/v1/paperwork")
public class PaperworkController {

    private final PaperworkService paperworkService;
    private final EnrollmentFormService enrollmentFormService;
    private final EnrollmentFormPdfExporter enrollmentFormPdfExporter;
    private final TenantRepository tenantRepository;
    private final TenantLogoService tenantLogoService;

    public PaperworkController(
            PaperworkService paperworkService,
            EnrollmentFormService enrollmentFormService,
            EnrollmentFormPdfExporter enrollmentFormPdfExporter,
            TenantRepository tenantRepository,
            TenantLogoService tenantLogoService) {
        this.paperworkService = paperworkService;
        this.enrollmentFormService = enrollmentFormService;
        this.enrollmentFormPdfExporter = enrollmentFormPdfExporter;
        this.tenantRepository = tenantRepository;
        this.tenantLogoService = tenantLogoService;
    }

    @GetMapping("/students/{studentId}/enrollment-certificate.pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'SECRETARY')")
    public ResponseEntity<byte[]> enrollmentCertificate(@PathVariable Long studentId) {
        return pdf(
                paperworkService.enrollmentCertificate(studentId),
                "certificat-scolarite-" + studentId + ".pdf");
    }

    /**
     * Fiche d'inscription à imprimer, faire signer au responsable légal et classer au
     * dossier. Pré-remplie de ce que l'application sait, avec des lignes vides pour ce
     * qu'elle ignore — lieu de naissance, adresse — que le guichet complète à la main.
     */
    @GetMapping("/students/{studentId}/enrollment-form.pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'SECRETARY')")
    public ResponseEntity<byte[]> enrollmentForm(@PathVariable Long studentId) {
        Tenant tenant = tenantRepository.findById(TenantContext.get())
                .orElseThrow(() -> ApiException.notFound("TENANT_NOT_FOUND", "Établissement introuvable"));
        byte[] contenu = enrollmentFormPdfExporter.export(
                enrollmentFormService.build(studentId),
                tenant,
                tenantLogoService.content(tenant).orElse(null));
        return pdf(contenu, "fiche-inscription-" + studentId + ".pdf");
    }

    @GetMapping("/payments/{paymentId}/receipt.pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'ACCOUNTANT')")
    public ResponseEntity<byte[]> paymentReceipt(@PathVariable Long paymentId) {
        return pdf(paperworkService.paymentReceipt(paymentId), "recu-" + paymentId + ".pdf");
    }

    private ResponseEntity<byte[]> pdf(byte[] content, String filename) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(filename).build().toString())
                .body(content);
    }
}
