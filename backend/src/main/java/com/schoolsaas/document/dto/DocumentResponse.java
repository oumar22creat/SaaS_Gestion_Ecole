package com.schoolsaas.document.dto;

import com.schoolsaas.document.Document;
import com.schoolsaas.document.DocumentScope;
import java.time.Instant;
import java.util.List;

public record DocumentResponse(
        Long id,
        String title,
        DocumentScope scope,
        Long subjectId,
        Long schoolClassId,
        String serviceLabel,
        String fileName,
        String contentType,
        long sizeBytes,
        boolean archived,
        Instant createdAt,
        List<String> visibleRoles) {

    public static DocumentResponse from(Document document, List<String> visibleRoles) {
        return new DocumentResponse(
                document.getId(),
                document.getTitle(),
                document.getScope(),
                document.getSubjectId(),
                document.getSchoolClassId(),
                document.getServiceLabel(),
                document.getFileName(),
                document.getContentType(),
                document.getSizeBytes(),
                document.isArchived(),
                document.getCreatedAt(),
                visibleRoles);
    }
}
