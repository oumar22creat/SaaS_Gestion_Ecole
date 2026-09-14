package com.schoolsaas.document;

import com.schoolsaas.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;

/** Document de la bibliothèque (cahier-des-charges.md §14). */
@Entity
@Table(name = "documents")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class Document extends TenantScopedEntity {

    @Column(nullable = false)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false)
    private DocumentScope scope;

    @Column(name = "subject_id", updatable = false)
    private Long subjectId;

    @Column(name = "school_class_id", updatable = false)
    private Long schoolClassId;

    @Column(name = "service_label", updatable = false)
    private String serviceLabel;

    @Column(name = "file_name", nullable = false, updatable = false)
    private String fileName;

    @Column(name = "content_type", nullable = false, updatable = false)
    private String contentType;

    @Column(name = "size_bytes", nullable = false, updatable = false)
    private long sizeBytes;

    @Column(name = "storage_key", nullable = false, updatable = false)
    private String storageKey;

    @Column(nullable = false)
    private boolean archived;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Document() {
    }

    public Document(
            String title, DocumentScope scope, Long subjectId, Long schoolClassId, String serviceLabel,
            String fileName, String contentType, long sizeBytes, String storageKey) {
        this.title = title;
        this.scope = scope;
        this.subjectId = subjectId;
        this.schoolClassId = schoolClassId;
        this.serviceLabel = serviceLabel;
        this.fileName = fileName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.storageKey = storageKey;
        this.archived = false;
        this.createdAt = Instant.now();
    }

    public String getTitle() {
        return title;
    }

    public DocumentScope getScope() {
        return scope;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public Long getSchoolClassId() {
        return schoolClassId;
    }

    public String getServiceLabel() {
        return serviceLabel;
    }

    public String getFileName() {
        return fileName;
    }

    public String getContentType() {
        return contentType;
    }

    public long getSizeBytes() {
        return sizeBytes;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
