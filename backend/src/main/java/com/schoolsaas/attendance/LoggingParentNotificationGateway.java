package com.schoolsaas.attendance;

import java.time.LocalDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Implémentation par défaut de {@link ParentNotificationGateway} : trace l'intention de
 * notifier (utile pour l'historique/le support) sans envoi réel — voir la javadoc de
 * l'interface. À remplacer par une implémentation FCM quand ROADMAP.md Phase 2 (§16) sera
 * construite.
 */
@Component
public class LoggingParentNotificationGateway implements ParentNotificationGateway {

    private static final Logger log = LoggerFactory.getLogger(LoggingParentNotificationGateway.class);

    @Override
    public void notifyAbsence(Long studentId, LocalDate date, AttendanceStatus status) {
        log.info("Notification parent (non envoyée, FCM pas encore câblé) : élève {} — {} le {}", studentId, status, date);
    }
}
