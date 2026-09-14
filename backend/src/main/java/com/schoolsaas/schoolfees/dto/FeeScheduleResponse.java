package com.schoolsaas.schoolfees.dto;

import com.schoolsaas.schoolfees.FeeSchedule;
import java.time.Instant;
import java.time.LocalDate;

public record FeeScheduleResponse(
        Long id, Long schoolClassId, String label, long amountCents, String currency, LocalDate dueDate, Instant createdAt) {

    public static FeeScheduleResponse from(FeeSchedule schedule) {
        return new FeeScheduleResponse(
                schedule.getId(), schedule.getSchoolClassId(), schedule.getLabel(), schedule.getAmountCents(),
                schedule.getCurrency(), schedule.getDueDate(), schedule.getCreatedAt());
    }
}
