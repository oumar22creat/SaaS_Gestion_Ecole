package com.schoolsaas.grade;

import com.schoolsaas.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;

/** Note d'un élève à une évaluation (cahier-des-charges.md §11). */
@Entity
@Table(name = "grades")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class Grade extends TenantScopedEntity {

    @Column(name = "exam_id", nullable = false, updatable = false)
    private Long examId;

    @Column(name = "student_id", nullable = false, updatable = false)
    private Long studentId;

    @Column
    private Double score;

    @Column(nullable = false)
    private boolean absent;

    @Column
    private String comment;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Grade() {
    }

    public Grade(Long examId, Long studentId, Double score, boolean absent, String comment) {
        this.examId = examId;
        this.studentId = studentId;
        this.score = absent ? null : score;
        this.absent = absent;
        this.comment = comment;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getExamId() {
        return examId;
    }

    public Long getStudentId() {
        return studentId;
    }

    public Double getScore() {
        return score;
    }

    public boolean isAbsent() {
        return absent;
    }

    public String getComment() {
        return comment;
    }

    public void update(Double score, boolean absent, String comment) {
        this.score = absent ? null : score;
        this.absent = absent;
        this.comment = comment;
        this.updatedAt = Instant.now();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
