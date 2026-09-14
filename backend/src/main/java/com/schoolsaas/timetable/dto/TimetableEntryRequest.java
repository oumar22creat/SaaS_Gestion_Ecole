package com.schoolsaas.timetable.dto;

import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record TimetableEntryRequest(
        @NotNull Long schoolClassId,
        @NotNull Long subjectId,
        @NotNull Long teacherId,
        @NotNull Long roomId,
        @NotNull DayOfWeek dayOfWeek,
        @NotNull LocalTime startTime,
        @NotNull LocalTime endTime) {
}
