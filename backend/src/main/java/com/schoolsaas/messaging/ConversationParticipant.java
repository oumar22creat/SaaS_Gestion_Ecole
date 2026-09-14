package com.schoolsaas.messaging;

import com.schoolsaas.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;

/** Participant d'une conversation — {@code lastReadAt} porte la confirmation de lecture. */
@Entity
@Table(name = "conversation_participants")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class ConversationParticipant extends TenantScopedEntity {

    @Column(name = "conversation_id", nullable = false, updatable = false)
    private Long conversationId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "last_read_at")
    private Instant lastReadAt;

    protected ConversationParticipant() {
    }

    public ConversationParticipant(Long conversationId, Long userId) {
        this.conversationId = conversationId;
        this.userId = userId;
    }

    public Long getConversationId() {
        return conversationId;
    }

    public Long getUserId() {
        return userId;
    }

    public Instant getLastReadAt() {
        return lastReadAt;
    }

    public void markRead(Instant at) {
        this.lastReadAt = at;
    }
}
