package com.schoolsaas.document;

import com.schoolsaas.auth.AuthenticatedPrincipal;
import com.schoolsaas.auth.Role;
import com.schoolsaas.common.ApiException;
import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.document.dto.DocumentResponse;
import java.util.List;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** Bibliothèque de documents — cahier-des-charges.md §14, ROADMAP.md 2.2. */
@RestController
@RequestMapping("/api/v1/documents")
@PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'TEACHER', 'SECRETARY', 'VIE_SCOLAIRE', 'ACCOUNTANT')")
public class DocumentController {

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'TEACHER', 'SECRETARY')")
    public ApiResponse<DocumentResponse> upload(
            @RequestParam String title,
            @RequestParam DocumentScope scope,
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long schoolClassId,
            @RequestParam(required = false) String serviceLabel,
            @RequestParam(required = false) List<Role> visibleRoles,
            @RequestParam("file") MultipartFile file) {
        Document document = documentService.upload(
                title, scope, subjectId, schoolClassId, serviceLabel, file, visibleRoles == null ? List.of() : visibleRoles);
        return ApiResponse.of(toResponse(document));
    }

    @GetMapping("/{id}")
    public ApiResponse<DocumentResponse> getById(@PathVariable Long id) {
        return ApiResponse.of(toResponse(documentService.getVisibleById(id, currentRole())));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id) {
        Document document = documentService.getVisibleById(id, currentRole());
        byte[] content = documentService.download(id, currentRole());
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(document.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(document.getFileName()).build().toString())
                .body(content);
    }

    @GetMapping
    public ApiResponse<List<DocumentResponse>> list(
            @RequestParam(required = false) Long subjectId,
            @RequestParam(required = false) Long schoolClassId,
            @RequestParam(required = false) String serviceLabel) {
        List<Document> documents;
        Role role = currentRole();
        if (subjectId != null) {
            documents = documentService.listBySubject(subjectId, role);
        } else if (schoolClassId != null) {
            documents = documentService.listByClass(schoolClassId, role);
        } else if (serviceLabel != null) {
            documents = documentService.listByService(serviceLabel, role);
        } else {
            throw ApiException.badRequest("MISSING_FILTER", "Fournir subjectId, schoolClassId ou serviceLabel", List.of());
        }
        return ApiResponse.of(documents.stream().map(this::toResponse).toList());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION', 'TEACHER', 'SECRETARY')")
    public void archive(@PathVariable Long id) {
        documentService.archive(id);
    }

    private DocumentResponse toResponse(Document document) {
        return DocumentResponse.from(document, documentService.visibleRolesOf(document.getId()));
    }

    private Role currentRole() {
        AuthenticatedPrincipal principal =
                (AuthenticatedPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return Role.valueOf(principal.role());
    }
}
