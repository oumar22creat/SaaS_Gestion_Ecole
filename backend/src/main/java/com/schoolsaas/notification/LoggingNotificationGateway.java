package com.schoolsaas.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implémentation par défaut de {@link NotificationGateway} : trace l'intention d'envoyer
 * (utile pour l'historique/le support) sans envoi réel — voir la javadoc de l'interface.
 */
@Component
public class LoggingNotificationGateway implements NotificationGateway {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationGateway.class);

    @Override
    public void send(NotificationEvent event) {
        log.info(
                "Notification (non envoyée, FCM pas encore câblé) : {} — \"{}\" — destinataires {}",
                event.type(), event.title(), event.recipientUserIds());
    }
}
