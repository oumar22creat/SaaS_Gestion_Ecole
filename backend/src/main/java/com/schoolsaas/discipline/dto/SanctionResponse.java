package com.schoolsaas.discipline.dto;

import com.schoolsaas.discipline.Sanction;
import com.schoolsaas.discipline.SanctionType;
import java.time.Instant;

public record SanctionResponse(
        Long id,
        Long incidentId,
        Long studentId,
        SanctionType type,
        Integer durationDays,
        String description,
        Long decidedByUserId,
        Instant decidedAt) {

    public static SanctionResponse from(Sanction sanction) {
        return new SanctionResponse(
                sanction.getId(), sanction.getIncidentId(), sanction.getStudentId(), sanction.getType(),
                sanction.getDurationDays(), sanction.getDescription(), sanction.getDecidedByUserId(), sanction.getDecidedAt());
    }
}
