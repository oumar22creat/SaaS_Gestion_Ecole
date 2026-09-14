package com.schoolsaas.messaging;

import java.util.List;

/** Notification "message reçu" (cahier-des-charges.md §16) — même pattern que les autres gateways, voir ROADMAP.md 2.5. */
public interface MessageNotificationGateway {

    void notifyNewMessage(Long conversationId, Long messageId, List<Long> recipientUserIds);
}
