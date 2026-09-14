package com.schoolsaas.student.dto;

import java.util.List;

public record StudentImportResult(int imported, List<RowError> errors) {

    public record RowError(int line, String message) {
    }
}
