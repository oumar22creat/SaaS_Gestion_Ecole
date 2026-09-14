package com.schoolsaas.grade;

import com.schoolsaas.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.Filter;

/** Évaluation (cahier-des-charges.md §11). */
@Entity
@Table(name = "exams")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class Exam extends TenantScopedEntity {

    @Column(name = "school_class_id", nullable = false)
    private Long schoolClassId;

    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Column(nullable = false)
    private String label;

    @Column(name = "max_score", nullable = false)
    private double maxScore;

    @Column(nullable = false)
    private int coefficient;

    @Column(name = "exam_date", nullable = false)
    private LocalDate examDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Exam() {
    }

    public Exam(Long schoolClassId, Long subjectId, String label, double maxScore, int coefficient, LocalDate examDate) {
        this.schoolClassId = schoolClassId;
        this.subjectId = subjectId;
        this.label = label;
        this.maxScore = maxScore;
        this.coefficient = coefficient;
        this.examDate = examDate;
        this.createdAt = Instant.now();
    }

    public Long getSchoolClassId() {
        return schoolClassId;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public double getMaxScore() {
        return maxScore;
    }

    public void setMaxScore(double maxScore) {
        this.maxScore = maxScore;
    }

    public int getCoefficient() {
        return coefficient;
    }

    public void setCoefficient(int coefficient) {
        this.coefficient = coefficient;
    }

    public LocalDate getExamDate() {
        return examDate;
    }

    public void setExamDate(LocalDate examDate) {
        this.examDate = examDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
