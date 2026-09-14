package com.schoolsaas.homework;

import com.schoolsaas.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.Filter;

/** Séance de cahier de textes : contenu du cours + travail à faire (cahier-des-charges.md §13). */
@Entity
@Table(name = "lessons")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class Lesson extends TenantScopedEntity {

    @Column(name = "school_class_id", nullable = false, updatable = false)
    private Long schoolClassId;

    @Column(name = "subject_id", nullable = false, updatable = false)
    private Long subjectId;

    @Column(name = "session_date", nullable = false)
    private LocalDate sessionDate;

    @Column(nullable = false)
    private String content;

    @Column
    private String homework;

    @Column(name = "homework_due_date")
    private LocalDate homeworkDueDate;

    @Column(name = "attachment_document_id")
    private Long attachmentDocumentId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Lesson() {
    }

    public Lesson(Long schoolClassId, Long subjectId, LocalDate sessionDate, String content) {
        this.schoolClassId = schoolClassId;
        this.subjectId = subjectId;
        this.sessionDate = sessionDate;
        this.content = content;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getSchoolClassId() {
        return schoolClassId;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public LocalDate getSessionDate() {
        return sessionDate;
    }

    public void setSessionDate(LocalDate sessionDate) {
        this.sessionDate = sessionDate;
        touch();
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        touch();
    }

    public String getHomework() {
        return homework;
    }

    public void setHomework(String homework) {
        this.homework = homework;
        touch();
    }

    public LocalDate getHomeworkDueDate() {
        return homeworkDueDate;
    }

    public void setHomeworkDueDate(LocalDate homeworkDueDate) {
        this.homeworkDueDate = homeworkDueDate;
        touch();
    }

    public Long getAttachmentDocumentId() {
        return attachmentDocumentId;
    }

    public void setAttachmentDocumentId(Long attachmentDocumentId) {
        this.attachmentDocumentId = attachmentDocumentId;
        touch();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
