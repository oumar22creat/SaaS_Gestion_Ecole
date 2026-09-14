package com.schoolsaas.parent.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record StudentParentLinkRequest(@NotNull Long parentId, @NotBlank String relationship, boolean primaryContact) {
}
