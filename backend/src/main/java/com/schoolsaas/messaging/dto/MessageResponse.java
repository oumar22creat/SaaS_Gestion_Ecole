package com.schoolsaas.messaging.dto;

import com.schoolsaas.messaging.Message;
import java.time.Instant;

public record MessageResponse(
        Long id, Long conversationId, Long senderId, String content, Long attachmentDocumentId, Instant createdAt) {

    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.getId(),
                message.getConversationId(),
                message.getSenderId(),
                message.getContent(),
                message.getAttachmentDocumentId(),
                message.getCreatedAt());
    }
}
