package com.schoolsaas.schoolyear.dto;

import com.schoolsaas.schoolyear.SchoolYear;
import com.schoolsaas.schoolyear.SchoolYearStatus;
import java.time.LocalDate;

public record SchoolYearResponse(
        Long id,
        String label,
        LocalDate startDate,
        LocalDate endDate,
        SchoolYearStatus status,
        long enrolledCount) {

    public static SchoolYearResponse from(SchoolYear year, long enrolledCount) {
        return new SchoolYearResponse(
                year.getId(), year.getLabel(), year.getStartDate(), year.getEndDate(), year.getStatus(), enrolledCount);
    }
}
