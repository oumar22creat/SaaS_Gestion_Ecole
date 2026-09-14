package com.schoolsaas.attendance;

import java.time.LocalDate;

/**
 * Notification au parent en cas d'absence/retard (cahier-des-charges.md §10). Frontière
 * volontairement séparée de {@link AttendanceService} (comme {@code StripeGateway} pour la
 * facturation) : l'envoi réel via Firebase Cloud Messaging (ROADMAP.md Phase 2, §16) n'est
 * pas encore câblé — voir {@link LoggingParentNotificationGateway} et
 * docs/ARCHITECTURE.md ADR-012 pour la raison (ni Redis ni FCM ne sont encore intégrés au
 * backend). Cette interface permet de brancher la vraie implémentation plus tard sans
 * toucher à {@link AttendanceService}.
 */
public interface ParentNotificationGateway {

    void notifyAbsence(Long studentId, LocalDate date, AttendanceStatus status);
}
