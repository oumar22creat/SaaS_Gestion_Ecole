package com.schoolsaas.discipline;

import com.schoolsaas.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

/** Élève impliqué dans un {@link Incident} — un incident peut concerner plusieurs élèves. */
@Entity
@Table(name = "incident_students")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class IncidentStudent extends TenantScopedEntity {

    @Column(name = "incident_id", nullable = false, updatable = false)
    private Long incidentId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private Long studentId;

    protected IncidentStudent() {
    }

    public IncidentStudent(Long incidentId, Long studentId) {
        this.incidentId = incidentId;
        this.studentId = studentId;
    }

    public Long getIncidentId() {
        return incidentId;
    }

    public Long getStudentId() {
        return studentId;
    }
}
