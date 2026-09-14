package com.schoolsaas.discipline.dto;

import com.schoolsaas.discipline.Incident;
import com.schoolsaas.discipline.IncidentSeverity;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public record IncidentResponse(
        Long id,
        Long schoolClassId,
        LocalDate occurredAt,
        IncidentSeverity severity,
        String description,
        Long reportedByUserId,
        List<Long> studentIds,
        Instant createdAt) {

    public static IncidentResponse from(Incident incident, List<Long> studentIds) {
        return new IncidentResponse(
                incident.getId(), incident.getSchoolClassId(), incident.getOccurredAt(), incident.getSeverity(),
                incident.getDescription(), incident.getReportedByUserId(), studentIds, incident.getCreatedAt());
    }
}
