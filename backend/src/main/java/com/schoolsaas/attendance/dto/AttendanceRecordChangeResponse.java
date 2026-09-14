package com.schoolsaas.attendance.dto;

import com.schoolsaas.attendance.AttendanceRecordChange;
import com.schoolsaas.attendance.AttendanceStatus;
import java.time.Instant;

public record AttendanceRecordChangeResponse(
        AttendanceStatus previousStatus, String previousReason, boolean previousJustified, String previousComment,
        Instant changedAt) {

    public static AttendanceRecordChangeResponse from(AttendanceRecordChange change) {
        return new AttendanceRecordChangeResponse(
                change.getPreviousStatus(),
                change.getPreviousReason(),
                change.isPreviousJustified(),
                change.getPreviousComment(),
                change.getChangedAt());
    }
}
