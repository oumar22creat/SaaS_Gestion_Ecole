package com.schoolsaas.attendance.dto;

import com.schoolsaas.attendance.AttendanceStatus;
import jakarta.validation.constraints.NotNull;

public record AttendanceRecordRequest(@NotNull AttendanceStatus status, String reason, boolean justified, String comment) {
}
