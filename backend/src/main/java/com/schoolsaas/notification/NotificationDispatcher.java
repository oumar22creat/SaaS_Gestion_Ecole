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
 * (usage global pour le dashboard Super-Admin, cahier §18, voir ADR-023). Applique aussi le
 * {@link NotificationTemplate} de l'établissement courant, s'il en existe un pour ce
 * {@link NotificationType} (cahier §2.4, ROADMAP.md 3.7, voir docs/ARCHITECTURE.md ADR-028) :
 * {@code titleOverride} remplace le titre calculé par le module appelant, et
 * {@code bodyTemplate} l'enveloppe (placeholder {@value NotificationTemplate#MESSAGE_PLACEHOLDER}) —
 * sans template configuré, titre et corps calculés par l'appelant restent inchangés.
 */
@Service
public class NotificationDispatcher {

    private final NotificationPreferenceRepository preferenceRepository;
    private final NotificationTemplateRepository templateRepository;
    private final NotificationGateway notificationGateway;
    private final NotificationLogRepository notificationLogRepository;

    public NotificationDispatcher(
            NotificationPreferenceRepository preferenceRepository,
            NotificationTemplateRepository templateRepository,
            NotificationGateway notificationGateway,
            NotificationLogRepository notificationLogRepository) {
        this.preferenceRepository = preferenceRepository;
        this.templateRepository = templateRepository;
        this.notificationGateway = notificationGateway;
        this.notificationLogRepository = notificationLogRepository;
    }

    /**
     * @param recipientUserIds comptes utilisateur à notifier ; vide si l'événement concerne un
     *                         destinataire sans compte (élève/parent, voir ADR-010) — l'envoi a
     *                         alors lieu sans filtrage par préférence.
     */
    public void dispatch(NotificationType type, List<Long> recipientUserIds, String title, String body) {
        String renderedTitle = applyTitleOverride(type, title);
        String renderedBody = applyBodyTemplate(type, body);
        if (recipientUserIds.isEmpty()) {
            notificationGateway.send(new NotificationEvent(type, recipientUserIds, renderedTitle, renderedBody));
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
        notificationGateway.send(new NotificationEvent(type, recipients, renderedTitle, renderedBody));
        logSent(type, recipients.size());
    }

    private String applyTitleOverride(NotificationType type, String defaultTitle) {
        return templateRepository.findByType(type)
                .map(NotificationTemplate::getTitleOverride)
                .filter(override -> override != null && !override.isBlank())
                .orElse(defaultTitle);
    }

    private String applyBodyTemplate(NotificationType type, String defaultBody) {
        return templateRepository.findByType(type)
                .map(NotificationTemplate::getBodyTemplate)
                .filter(template -> template != null && !template.isBlank())
                .map(template -> template.replace(NotificationTemplate.MESSAGE_PLACEHOLDER, defaultBody))
                .orElse(defaultBody);
    }

    private void logSent(NotificationType type, int recipientCount) {
        notificationLogRepository.save(new NotificationLog(TenantContext.get(), type, recipientCount));
    }
}
