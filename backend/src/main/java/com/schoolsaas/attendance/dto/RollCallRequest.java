package com.schoolsaas.attendance.dto;

import com.schoolsaas.attendance.AttendanceStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/** Feuille d'appel (cahier-des-charges.md §10) : une soumission = tous les élèves d'une classe pour un jour donné. */
public record RollCallRequest(
        @NotNull Long schoolClassId, @NotNull LocalDate date, @NotEmpty @Valid List<Entry> entries) {

    public record Entry(
            @NotNull Long studentId, @NotNull AttendanceStatus status, String reason, boolean justified, String comment) {
    }
}
