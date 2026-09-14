package com.schoolsaas.timetable.dto;

import com.schoolsaas.timetable.TimetableEntry;
import java.time.DayOfWeek;
import java.time.LocalTime;

public record TimetableEntryResponse(
        Long id,
        Long schoolClassId,
        Long subjectId,
        Long teacherId,
        Long roomId,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime) {

    public static TimetableEntryResponse from(TimetableEntry entry) {
        return new TimetableEntryResponse(
                entry.getId(),
                entry.getSchoolClassId(),
                entry.getSubjectId(),
                entry.getTeacherId(),
                entry.getRoomId(),
                entry.getDayOfWeek(),
                entry.getStartTime(),
                entry.getEndTime());
    }
}
