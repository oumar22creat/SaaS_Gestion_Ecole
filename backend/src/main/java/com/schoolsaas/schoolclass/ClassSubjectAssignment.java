package com.schoolsaas.schoolclass;

import com.schoolsaas.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;

/** Affectation enseignant/classe/matière (cahier-des-charges.md §8). */
@Entity
@Table(name = "class_subject_assignments")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class ClassSubjectAssignment extends TenantScopedEntity {

    @Column(name = "class_id", nullable = false, updatable = false)
    private Long classId;

    @Column(name = "subject_id", nullable = false, updatable = false)
    private Long subjectId;

    @Column(name = "teacher_id", nullable = false)
    private Long teacherId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected ClassSubjectAssignment() {
    }

    public ClassSubjectAssignment(Long classId, Long subjectId, Long teacherId) {
        this.classId = classId;
        this.subjectId = subjectId;
        this.teacherId = teacherId;
        this.createdAt = Instant.now();
    }

    public Long getClassId() {
        return classId;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public Long getTeacherId() {
        return teacherId;
    }

    public void setTeacherId(Long teacherId) {
        this.teacherId = teacherId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
