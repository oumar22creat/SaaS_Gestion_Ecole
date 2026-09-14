package com.schoolsaas.library.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;

public record BookCreateRequest(@NotBlank String barcode, String isbn, @NotBlank String title, @NotBlank String author, @Positive int totalCopies) {
}
