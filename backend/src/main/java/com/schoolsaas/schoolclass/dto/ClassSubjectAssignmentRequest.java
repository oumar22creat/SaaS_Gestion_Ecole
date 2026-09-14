package com.schoolsaas.schoolclass.dto;

import jakarta.validation.constraints.NotNull;

public record ClassSubjectAssignmentRequest(@NotNull Long subjectId, @NotNull Long teacherId) {
}
