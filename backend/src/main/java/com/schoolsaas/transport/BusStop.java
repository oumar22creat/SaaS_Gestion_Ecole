package com.schoolsaas.transport;

import com.schoolsaas.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;

/** Arrêt d'une {@link BusRoute}, ordonné par {@code sequenceOrder} (cahier-des-charges.md §19.2). */
@Entity
@Table(name = "bus_stops")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class BusStop extends TenantScopedEntity {

    @Column(name = "bus_route_id", nullable = false, updatable = false)
    private Long busRouteId;

    @Column(nullable = false)
    private String name;

    @Column(name = "sequence_order", nullable = false)
    private int sequenceOrder;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BusStop() {
    }

    public BusStop(Long busRouteId, String name, int sequenceOrder) {
        this.busRouteId = busRouteId;
        this.name = name;
        this.sequenceOrder = sequenceOrder;
        this.createdAt = Instant.now();
    }

    public Long getBusRouteId() {
        return busRouteId;
    }

    public String getName() {
        return name;
    }

    public int getSequenceOrder() {
        return sequenceOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
