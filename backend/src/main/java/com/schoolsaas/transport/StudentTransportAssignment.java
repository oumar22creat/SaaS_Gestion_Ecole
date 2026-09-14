package com.schoolsaas.transport;

import com.schoolsaas.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;

/** Affectation d'un élève à un circuit/arrêt (cahier-des-charges.md §19.2), une seule à la fois. */
@Entity
@Table(name = "student_transport_assignments")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class StudentTransportAssignment extends TenantScopedEntity {

    @Column(name = "student_id", nullable = false, updatable = false)
    private Long studentId;

    @Column(name = "bus_route_id", nullable = false)
    private Long busRouteId;

    @Column(name = "bus_stop_id", nullable = false)
    private Long busStopId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected StudentTransportAssignment() {
    }

    public StudentTransportAssignment(Long studentId, Long busRouteId, Long busStopId) {
        this.studentId = studentId;
        this.busRouteId = busRouteId;
        this.busStopId = busStopId;
        this.createdAt = Instant.now();
    }

    public Long getStudentId() {
        return studentId;
    }

    public Long getBusRouteId() {
        return busRouteId;
    }

    public void setBusRouteId(Long busRouteId) {
        this.busRouteId = busRouteId;
    }

    public Long getBusStopId() {
        return busStopId;
    }

    public void setBusStopId(Long busStopId) {
        this.busStopId = busStopId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
