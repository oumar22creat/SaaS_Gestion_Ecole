package com.schoolsaas.attendance;

import com.schoolsaas.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;

/** Instantané de l'état précédent d'un {@link AttendanceRecord} avant une modification. */
@Entity
@Table(name = "attendance_record_changes")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class AttendanceRecordChange extends TenantScopedEntity {

    @Column(name = "attendance_record_id", nullable = false, updatable = false)
    private Long attendanceRecordId;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", nullable = false, updatable = false)
    private AttendanceStatus previousStatus;

    @Column(name = "previous_reason", updatable = false)
    private String previousReason;

    @Column(name = "previous_justified", nullable = false, updatable = false)
    private boolean previousJustified;

    @Column(name = "previous_comment", updatable = false)
    private String previousComment;

    @Column(name = "changed_at", nullable = false, updatable = false)
    private Instant changedAt;

    protected AttendanceRecordChange() {
    }

    public AttendanceRecordChange(
            Long attendanceRecordId, AttendanceStatus previousStatus, String previousReason, boolean previousJustified,
            String previousComment) {
        this.attendanceRecordId = attendanceRecordId;
        this.previousStatus = previousStatus;
        this.previousReason = previousReason;
        this.previousJustified = previousJustified;
        this.previousComment = previousComment;
        this.changedAt = Instant.now();
    }

    public Long getAttendanceRecordId() {
        return attendanceRecordId;
    }

    public AttendanceStatus getPreviousStatus() {
        return previousStatus;
    }

    public String getPreviousReason() {
        return previousReason;
    }

    public boolean isPreviousJustified() {
        return previousJustified;
    }

    public String getPreviousComment() {
        return previousComment;
    }

    public Instant getChangedAt() {
        return changedAt;
    }
}
