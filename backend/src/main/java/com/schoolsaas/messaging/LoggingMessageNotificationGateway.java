package com.schoolsaas.messaging;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class LoggingMessageNotificationGateway implements MessageNotificationGateway {

    private static final Logger log = LoggerFactory.getLogger(LoggingMessageNotificationGateway.class);

    @Override
    public void notifyNewMessage(Long conversationId, Long messageId, List<Long> recipientUserIds) {
        log.info(
                "Notification message reçu (non envoyée, FCM pas encore câblé) : conversation {} — message {} — destinataires {}",
                conversationId, messageId, recipientUserIds);
    }
}
