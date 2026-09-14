package com.schoolsaas.transport.dto;

import com.schoolsaas.transport.StudentTransportAssignment;

public record TransportAssignmentResponse(Long id, Long studentId, Long busRouteId, Long busStopId) {

    public static TransportAssignmentResponse from(StudentTransportAssignment assignment) {
        return new TransportAssignmentResponse(
                assignment.getId(), assignment.getStudentId(), assignment.getBusRouteId(), assignment.getBusStopId());
    }
}
