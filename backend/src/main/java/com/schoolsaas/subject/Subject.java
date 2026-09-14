package com.schoolsaas.subject;

import com.schoolsaas.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;

/** Matière (cahier-des-charges.md §8). */
@Entity
@Table(name = "subjects")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class Subject extends TenantScopedEntity {

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private int coefficient;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Subject() {
    }

    public Subject(String name, String code, int coefficient) {
        this.name = name;
        this.code = code;
        this.coefficient = coefficient;
        this.createdAt = Instant.now();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public int getCoefficient() {
        return coefficient;
    }

    public void setCoefficient(int coefficient) {
        this.coefficient = coefficient;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
