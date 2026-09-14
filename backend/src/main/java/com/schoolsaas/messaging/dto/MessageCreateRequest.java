package com.schoolsaas.messaging.dto;

import jakarta.validation.constraints.NotBlank;

public record MessageCreateRequest(@NotBlank String content, Long attachmentDocumentId) {
}
