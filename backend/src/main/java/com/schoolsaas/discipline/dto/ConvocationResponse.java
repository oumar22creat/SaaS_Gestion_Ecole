package com.schoolsaas.discipline.dto;

import com.schoolsaas.discipline.Convocation;
import com.schoolsaas.discipline.ConvocationStatus;
import java.time.Instant;

public record ConvocationResponse(
        Long id,
        Long studentId,
        boolean convokeParent,
        String reason,
        Instant scheduledAt,
        ConvocationStatus status,
        Long createdByUserId,
        Instant createdAt) {

    public static ConvocationResponse from(Convocation convocation) {
        return new ConvocationResponse(
                convocation.getId(), convocation.getStudentId(), convocation.isConvokeParent(), convocation.getReason(),
                convocation.getScheduledAt(), convocation.getStatus(), convocation.getCreatedByUserId(), convocation.getCreatedAt());
    }
}
