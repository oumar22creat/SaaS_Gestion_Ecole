package com.schoolsaas.messaging;

import com.schoolsaas.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;

/** Conversation (individuelle, de groupe, ou annonce) — cahier-des-charges.md §15. */
@Entity
@Table(name = "conversations")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class Conversation extends TenantScopedEntity {

    @Column
    private String title;

    @Column(name = "is_announcement", nullable = false, updatable = false)
    private boolean announcement;

    @Column(name = "created_by_user_id", nullable = false, updatable = false)
    private Long createdByUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Conversation() {
    }

    public Conversation(String title, boolean announcement, Long createdByUserId) {
        this.title = title;
        this.announcement = announcement;
        this.createdByUserId = createdByUserId;
        this.createdAt = Instant.now();
    }

    public String getTitle() {
        return title;
    }

    public boolean isAnnouncement() {
        return announcement;
    }

    public Long getCreatedByUserId() {
        return createdByUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
