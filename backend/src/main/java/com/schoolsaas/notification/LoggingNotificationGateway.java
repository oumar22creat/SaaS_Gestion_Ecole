package com.schoolsaas.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Implémentation par défaut de {@link NotificationGateway} : trace l'intention d'envoyer
 * (utile pour l'historique et le support) sans envoi réel.
 *
 * <p>Active tant que {@code app.fcm.enabled} vaut faux — le réglage par défaut. Même parti
 * pris que {@code LoggingSmsGateway} : un établissement sans identifiants Firebase doit
 * fonctionner normalement, notifications en moins, plutôt que refuser de démarrer.
 */
@Component
@ConditionalOnProperty(prefix = "app.fcm", name = "enabled", havingValue = "false", matchIfMissing = true)
public class LoggingNotificationGateway implements NotificationGateway {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationGateway.class);

    @Override
    public void send(NotificationEvent event) {
        log.info(
                "Notification (non envoyée, push désactivé) : {} — \"{}\" — \"{}\" — destinataires {}",
                event.type(), event.title(), event.body(), event.recipientUserIds());
    }
}
