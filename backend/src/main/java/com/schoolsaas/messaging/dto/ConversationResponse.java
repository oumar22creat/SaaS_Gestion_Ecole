package com.schoolsaas.messaging.dto;

import com.schoolsaas.messaging.Conversation;
import java.time.Instant;
import java.util.List;

public record ConversationResponse(
        Long id, String title, boolean announcement, Long createdByUserId, Instant createdAt,
        List<Long> participantUserIds, long unreadCount) {

    public static ConversationResponse from(Conversation conversation, List<Long> participantUserIds, long unreadCount) {
        return new ConversationResponse(
                conversation.getId(),
                conversation.getTitle(),
                conversation.isAnnouncement(),
                conversation.getCreatedByUserId(),
                conversation.getCreatedAt(),
                participantUserIds,
                unreadCount);
    }
}
