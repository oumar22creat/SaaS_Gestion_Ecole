package com.schoolsaas.homework.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;

public record LessonRequest(
        @NotNull Long schoolClassId,
        @NotNull Long subjectId,
        @NotNull LocalDate sessionDate,
        @NotBlank String content,
        String homework,
        LocalDate homeworkDueDate,
        Long attachmentDocumentId) {
}
