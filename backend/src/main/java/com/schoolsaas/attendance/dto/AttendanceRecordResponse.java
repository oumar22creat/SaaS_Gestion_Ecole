package com.schoolsaas.attendance.dto;

import com.schoolsaas.attendance.AttendanceRecord;
import com.schoolsaas.attendance.AttendanceStatus;
import java.time.LocalDate;

public record AttendanceRecordResponse(
        Long id,
        Long studentId,
        Long schoolClassId,
        LocalDate date,
        AttendanceStatus status,
        String reason,
        boolean justified,
        String comment,
        boolean parentNotified) {

    public static AttendanceRecordResponse from(AttendanceRecord record) {
        return new AttendanceRecordResponse(
                record.getId(),
                record.getStudentId(),
                record.getSchoolClassId(),
                record.getDate(),
                record.getStatus(),
                record.getReason(),
                record.isJustified(),
                record.getComment(),
                record.isParentNotified());
    }
}
