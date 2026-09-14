package com.schoolsaas.discipline.dto;

import com.schoolsaas.discipline.Observation;
import java.time.Instant;

public record ObservationResponse(Long id, Long studentId, boolean positive, String description, Long authorUserId, Instant createdAt) {

    public static ObservationResponse from(Observation observation) {
        return new ObservationResponse(
                observation.getId(), observation.getStudentId(), observation.isPositive(), observation.getDescription(),
                observation.getAuthorUserId(), observation.getCreatedAt());
    }
}
