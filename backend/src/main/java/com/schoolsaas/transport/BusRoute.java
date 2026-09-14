package com.schoolsaas.transport;

import com.schoolsaas.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;

/** Ligne de bus (cahier-des-charges.md §19.2). */
@Entity
@Table(name = "bus_routes")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class BusRoute extends TenantScopedEntity {

    @Column(nullable = false)
    private String label;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected BusRoute() {
    }

    public BusRoute(String label) {
        this.label = label;
        this.createdAt = Instant.now();
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
