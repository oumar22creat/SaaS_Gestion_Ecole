package com.schoolsaas.reportcard;

import com.schoolsaas.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

/** Ligne "matière" d'un bulletin — moyenne et coefficient figés à la génération. */
@Entity
@Table(name = "report_card_entries")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class ReportCardEntry extends TenantScopedEntity {

    @Column(name = "report_card_id", nullable = false, updatable = false)
    private Long reportCardId;

    @Column(name = "subject_id", nullable = false, updatable = false)
    private Long subjectId;

    @Column
    private Double average;

    @Column(nullable = false, updatable = false)
    private int coefficient;

    @Column(name = "teacher_comment")
    private String teacherComment;

    protected ReportCardEntry() {
    }

    public ReportCardEntry(Long reportCardId, Long subjectId, Double average, int coefficient) {
        this.reportCardId = reportCardId;
        this.subjectId = subjectId;
        this.average = average;
        this.coefficient = coefficient;
    }

    public Long getReportCardId() {
        return reportCardId;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public Double getAverage() {
        return average;
    }

    public int getCoefficient() {
        return coefficient;
    }

    public String getTeacherComment() {
        return teacherComment;
    }

    public void setTeacherComment(String teacherComment) {
        this.teacherComment = teacherComment;
    }
}
