package com.schoolsaas.paperwork;

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

    public PaperworkController(PaperworkService paperworkService) {
        this.paperworkService = paperworkService;
    }

    @GetMapping("/students/{studentId}/enrollment-certificate.pdf")
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'SECRETARY')")
    public ResponseEntity<byte[]> enrollmentCertificate(@PathVariable Long studentId) {
        return pdf(
                paperworkService.enrollmentCertificate(studentId),
                "certificat-scolarite-" + studentId + ".pdf");
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
