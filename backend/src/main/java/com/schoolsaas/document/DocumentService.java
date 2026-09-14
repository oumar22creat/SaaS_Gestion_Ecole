package com.schoolsaas.document;

import com.schoolsaas.auth.Role;
import com.schoolsaas.common.ApiException;
import java.io.IOException;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/** Bibliothèque de documents — cahier-des-charges.md §14, ROADMAP.md 2.2. */
@Service
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentVisibleRoleRepository visibleRoleRepository;
    private final StorageGateway storageGateway;

    public DocumentService(
            DocumentRepository documentRepository, DocumentVisibleRoleRepository visibleRoleRepository, StorageGateway storageGateway) {
        this.documentRepository = documentRepository;
        this.visibleRoleRepository = visibleRoleRepository;
        this.storageGateway = storageGateway;
    }

    @Transactional
    public Document upload(
            String title, DocumentScope scope, Long subjectId, Long schoolClassId, String serviceLabel,
            MultipartFile file, List<Role> visibleRoles) {
        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException e) {
            throw ApiException.badRequest("INVALID_FILE", "Impossible de lire le fichier envoyé", List.of());
        }
        String storageKey = storageGateway.store(content, file.getOriginalFilename());
        Document document = documentRepository.save(new Document(
                title, scope, subjectId, schoolClassId, serviceLabel,
                file.getOriginalFilename(), file.getContentType(), file.getSize(), storageKey));

        for (Role role : visibleRoles) {
            visibleRoleRepository.save(new DocumentVisibleRole(document.getId(), role));
        }
        return document;
    }

    public Document getById(Long id) {
        return documentRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("DOCUMENT_NOT_FOUND", "Document introuvable"));
    }

    /** Comme {@link #getById} mais applique les droits de consultation (cahier §14). */
    public Document getVisibleById(Long id, Role callerRole) {
        Document document = getById(id);
        if (!isVisibleTo(id, callerRole)) {
            throw ApiException.forbidden("DOCUMENT_NOT_VISIBLE", "Ce document n'est pas consultable avec votre rôle");
        }
        return document;
    }

    public List<String> visibleRolesOf(Long documentId) {
        return visibleRoleRepository.findAllByDocumentId(documentId).stream().map(r -> r.getRole().name()).toList();
    }

    public byte[] download(Long id, Role callerRole) {
        return storageGateway.retrieve(getVisibleById(id, callerRole).getStorageKey());
    }

    public List<Document> listBySubject(Long subjectId, Role callerRole) {
        return filterVisible(documentRepository.findAllByArchivedFalseAndScopeAndSubjectId(DocumentScope.SUBJECT, subjectId), callerRole);
    }

    public List<Document> listByClass(Long schoolClassId, Role callerRole) {
        return filterVisible(
                documentRepository.findAllByArchivedFalseAndScopeAndSchoolClassId(DocumentScope.CLASS, schoolClassId), callerRole);
    }

    public List<Document> listByService(String serviceLabel, Role callerRole) {
        return filterVisible(
                documentRepository.findAllByArchivedFalseAndScopeAndServiceLabel(DocumentScope.SERVICE, serviceLabel), callerRole);
    }

    /** Ne garde que les documents consultables par le rôle appelant (cahier §14 : "droits de consultation"). */
    private List<Document> filterVisible(List<Document> documents, Role callerRole) {
        return documents.stream().filter(document -> isVisibleTo(document.getId(), callerRole)).toList();
    }

    public boolean isVisibleTo(Long documentId, Role role) {
        List<DocumentVisibleRole> restrictions = visibleRoleRepository.findAllByDocumentId(documentId);
        return restrictions.isEmpty() || restrictions.stream().anyMatch(r -> r.getRole() == role);
    }

    @Transactional
    public void archive(Long id) {
        Document document = getById(id);
        document.setArchived(true);
    }
}
