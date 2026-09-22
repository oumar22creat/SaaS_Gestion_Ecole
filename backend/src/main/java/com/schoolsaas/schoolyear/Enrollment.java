package com.schoolsaas.schoolyear;

import com.schoolsaas.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.Filter;

/**
 * Inscription d'un élève pour une année donnée. C'est ici que vit l'historique : la colonne
 * {@code students.school_class_id} ne retient que la classe courante et s'écrase à chaque
 * changement.
 */
@Entity
@Table(name = "enrollments")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class Enrollment extends TenantScopedEntity {

    @Column(name = "school_year_id", nullable = false, updatable = false)
    private Long schoolYearId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private Long studentId;

    @Column(name = "school_class_id", nullable = false)
    private Long schoolClassId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EnrollmentStatus status;

    @Column(name = "enrolled_at", nullable = false)
    private LocalDate enrolledAt;

    @Column(name = "left_at")
    private LocalDate leftAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Enrollment() {
    }

    public Enrollment(Long schoolYearId, Long studentId, Long schoolClassId, LocalDate enrolledAt) {
        this.schoolYearId = schoolYearId;
        this.studentId = studentId;
        this.schoolClassId = schoolClassId;
        this.status = EnrollmentStatus.ENROLLED;
        this.enrolledAt = enrolledAt;
        this.createdAt = Instant.now();
    }

    public Long getSchoolYearId() {
        return schoolYearId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public Long getSchoolClassId() {
        return schoolClassId;
    }

    public void setSchoolClassId(Long schoolClassId) {
        this.schoolClassId = schoolClassId;
    }

    public EnrollmentStatus getStatus() {
        return status;
    }

    public void setStatus(EnrollmentStatus status) {
        this.status = status;
    }

    public LocalDate getEnrolledAt() {
        return enrolledAt;
    }

    public LocalDate getLeftAt() {
        return leftAt;
    }

    public void setLeftAt(LocalDate leftAt) {
        this.leftAt = leftAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
