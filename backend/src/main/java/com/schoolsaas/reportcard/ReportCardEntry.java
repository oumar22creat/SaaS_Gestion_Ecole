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

    /**
     * Le détail que lit la famille, figé comme la moyenne l'est déjà : le bulletin réédité
     * six mois plus tard doit rendre le même document, même si une note a été corrigée
     * depuis. Null quand l'évaluation n'a pas eu lieu — une case vide se lit mieux qu'un
     * zéro, qui serait compris comme une note.
     */
    @Column(name = "assignment_one_score")
    private Double assignmentOneScore;

    @Column(name = "assignment_two_score")
    private Double assignmentTwoScore;

    /** Composition de fin de période. */
    @Column(name = "exam_score")
    private Double examScore;

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

    public Double getAssignmentOneScore() {
        return assignmentOneScore;
    }

    public Double getAssignmentTwoScore() {
        return assignmentTwoScore;
    }

    public Double getExamScore() {
        return examScore;
    }

    public void setScores(Double assignmentOne, Double assignmentTwo, Double examScore) {
        this.assignmentOneScore = assignmentOne;
        this.assignmentTwoScore = assignmentTwo;
        this.examScore = examScore;
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
