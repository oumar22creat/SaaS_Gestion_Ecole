package com.schoolsaas.reportcard;

import com.schoolsaas.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.Filter;

/** Bulletin scolaire d'un élève pour une période (cahier-des-charges.md §12). */
@Entity
@Table(name = "report_cards")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class ReportCard extends TenantScopedEntity {

    @Column(name = "student_id", nullable = false, updatable = false)
    private Long studentId;

    @Column(name = "school_class_id", nullable = false)
    private Long schoolClassId;

    @Column(name = "period_label", nullable = false, updatable = false)
    private String periodLabel;

    @Column(name = "period_from", nullable = false)
    private LocalDate periodFrom;

    @Column(name = "period_to", nullable = false)
    private LocalDate periodTo;

    @Column(name = "general_average")
    private Double generalAverage;

    @Column(name = "general_comment")
    private String generalComment;

    @Column(name = "council_decision")
    private String councilDecision;

    /**
     * Rang dans la classe, sur la moyenne générale. Stocké et non recalculé à la lecture : un
     * bulletin est remis à une date donnée, son rang doit rester celui du conseil de classe
     * même si une note est corrigée ensuite.
     */
    @Column(name = "rank_in_class")
    private Integer rankInClass;

    /** Effectif pris en compte pour le rang : « 3e » ne veut rien dire sans « sur 42 ». */
    @Column(name = "class_size")
    private Integer classSize;

    @Column(name = "absence_count", nullable = false)
    private int absenceCount;

    @Column(name = "late_count", nullable = false)
    private int lateCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ReportCard() {
    }

    public ReportCard(Long studentId, Long schoolClassId, String periodLabel, LocalDate periodFrom, LocalDate periodTo) {
        this.studentId = studentId;
        this.schoolClassId = schoolClassId;
        this.periodLabel = periodLabel;
        this.periodFrom = periodFrom;
        this.periodTo = periodTo;
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public Long getStudentId() {
        return studentId;
    }

    public Long getSchoolClassId() {
        return schoolClassId;
    }

    public String getPeriodLabel() {
        return periodLabel;
    }

    public LocalDate getPeriodFrom() {
        return periodFrom;
    }

    public LocalDate getPeriodTo() {
        return periodTo;
    }

    public Double getGeneralAverage() {
        return generalAverage;
    }

    public void setGeneralAverage(Double generalAverage) {
        this.generalAverage = generalAverage;
        touch();
    }

    public String getGeneralComment() {
        return generalComment;
    }

    public void setGeneralComment(String generalComment) {
        this.generalComment = generalComment;
        touch();
    }

    public String getCouncilDecision() {
        return councilDecision;
    }

    public void setCouncilDecision(String councilDecision) {
        this.councilDecision = councilDecision;
        touch();
    }

    public Integer getRankInClass() {
        return rankInClass;
    }

    public void setRankInClass(Integer rankInClass) {
        this.rankInClass = rankInClass;
    }

    public Integer getClassSize() {
        return classSize;
    }

    public void setClassSize(Integer classSize) {
        this.classSize = classSize;
    }

    public int getAbsenceCount() {
        return absenceCount;
    }

    public void setAbsenceCount(int absenceCount) {
        this.absenceCount = absenceCount;
        touch();
    }

    public int getLateCount() {
        return lateCount;
    }

    public void setLateCount(int lateCount) {
        this.lateCount = lateCount;
        touch();
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private void touch() {
        this.updatedAt = Instant.now();
    }
}
