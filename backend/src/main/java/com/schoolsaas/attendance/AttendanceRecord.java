package com.schoolsaas.attendance;

import com.schoolsaas.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDate;
import org.hibernate.annotations.Filter;

/** Présence d'un élève pour une journée (cahier-des-charges.md §10). */
@Entity
@Table(name = "attendance_records")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class AttendanceRecord extends TenantScopedEntity {

    @Column(name = "student_id", nullable = false, updatable = false)
    private Long studentId;

    @Column(name = "school_class_id", nullable = false)
    private Long schoolClassId;

    @Column(nullable = false, updatable = false)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AttendanceStatus status;

    @Column
    private String reason;

    @Column(nullable = false)
    private boolean justified;

    @Column
    private String comment;

    @Column(name = "parent_notified", nullable = false)
    private boolean parentNotified;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected AttendanceRecord() {
    }

    public AttendanceRecord(
            Long studentId, Long schoolClassId, LocalDate date, AttendanceStatus status, String reason, boolean justified,
            String comment) {
        this.studentId = studentId;
        this.schoolClassId = schoolClassId;
        this.date = date;
        this.status = status;
        this.reason = reason;
        this.justified = justified;
        this.comment = comment;
        this.parentNotified = false;
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

    public void setSchoolClassId(Long schoolClassId) {
        this.schoolClassId = schoolClassId;
    }

    public LocalDate getDate() {
        return date;
    }

    public AttendanceStatus getStatus() {
        return status;
    }

    public void setStatus(AttendanceStatus status) {
        this.status = status;
        touch();
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
        touch();
    }

    public boolean isJustified() {
        return justified;
    }

    public void setJustified(boolean justified) {
        this.justified = justified;
        touch();
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
        touch();
    }

    public boolean isParentNotified() {
        return parentNotified;
    }

    public void setParentNotified(boolean parentNotified) {
        this.parentNotified = parentNotified;
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
