package com.schoolsaas.notification;

import com.schoolsaas.tenant.TenantContext;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * Registre centralisé des notifications (ROADMAP.md 2.5) : point d'entrée unique utilisé par
 * tous les modules métier (absences, devoirs, messagerie, notes, documents, abonnement) pour
 * déclencher une notification, à la place des gateways ad hoc précédemment dupliqués par
 * module (voir docs/ARCHITECTURE.md ADR-020). Applique les préférences utilisateur avant
 * d'appeler {@link NotificationGateway}, puis journalise l'envoi dans {@link NotificationLog}
 * (usage global pour le dashboard Super-Admin, cahier §18, voir ADR-023).
 */
@Service
public class NotificationDispatcher {

    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationGateway notificationGateway;
    private final NotificationLogRepository notificationLogRepository;

    public NotificationDispatcher(
            NotificationPreferenceRepository preferenceRepository,
            NotificationGateway notificationGateway,
            NotificationLogRepository notificationLogRepository) {
        this.preferenceRepository = preferenceRepository;
        this.notificationGateway = notificationGateway;
        this.notificationLogRepository = notificationLogRepository;
    }

    /**
     * @param recipientUserIds comptes utilisateur à notifier ; vide si l'événement concerne un
     *                         destinataire sans compte (élève/parent, voir ADR-010) — l'envoi a
     *                         alors lieu sans filtrage par préférence.
     */
    public void dispatch(NotificationType type, List<Long> recipientUserIds, String title, String body) {
        if (recipientUserIds.isEmpty()) {
            notificationGateway.send(new NotificationEvent(type, recipientUserIds, title, body));
            logSent(type, recipientUserIds.size());
            return;
        }
        Set<Long> optedOut = preferenceRepository.findAllByUserIdInAndType(recipientUserIds, type).stream()
                .filter(p -> !p.isEnabled())
                .map(NotificationPreference::getUserId)
                .collect(Collectors.toSet());
        List<Long> recipients = recipientUserIds.stream().filter(id -> !optedOut.contains(id)).toList();
        if (recipients.isEmpty()) {
            return;
        }
        notificationGateway.send(new NotificationEvent(type, recipients, title, body));
        logSent(type, recipients.size());
    }

    private void logSent(NotificationType type, int recipientCount) {
        notificationLogRepository.save(new NotificationLog(TenantContext.get(), type, recipientCount));
    }
}
