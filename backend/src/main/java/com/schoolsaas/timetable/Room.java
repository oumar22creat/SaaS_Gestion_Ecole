package com.schoolsaas.timetable;

import com.schoolsaas.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;

/** Salle (cahier-des-charges.md §9). */
@Entity
@Table(name = "rooms")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class Room extends TenantScopedEntity {

    @Column(nullable = false)
    private String name;

    @Column
    private Integer capacity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Room() {
    }

    public Room(String name, Integer capacity) {
        this.name = name;
        this.capacity = capacity;
        this.createdAt = Instant.now();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
