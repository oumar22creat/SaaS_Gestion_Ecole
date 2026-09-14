package com.schoolsaas.messaging;

import com.schoolsaas.common.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.time.Instant;
import org.hibernate.annotations.Filter;

/** Message d'une conversation — cahier-des-charges.md §15. */
@Entity
@Table(name = "messages")
@Filter(name = "tenantFilter", condition = "school_id = :tenantId")
public class Message extends TenantScopedEntity {

    @Column(name = "conversation_id", nullable = false, updatable = false)
    private Long conversationId;

    @Column(name = "sender_id", nullable = false, updatable = false)
    private Long senderId;

    @Column(nullable = false, updatable = false)
    private String content;

    @Column(name = "attachment_document_id", updatable = false)
    private Long attachmentDocumentId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected Message() {
    }

    public Message(Long conversationId, Long senderId, String content, Long attachmentDocumentId) {
        this.conversationId = conversationId;
        this.senderId = senderId;
        this.content = content;
        this.attachmentDocumentId = attachmentDocumentId;
        this.createdAt = Instant.now();
    }

    public Long getConversationId() {
        return conversationId;
    }

    public Long getSenderId() {
        return senderId;
    }

    public String getContent() {
        return content;
    }

    public Long getAttachmentDocumentId() {
        return attachmentDocumentId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
