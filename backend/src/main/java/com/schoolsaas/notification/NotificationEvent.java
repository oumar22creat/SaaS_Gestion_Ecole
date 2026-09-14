package com.schoolsaas.notification;

import java.util.List;

/**
 * Événement de notification prêt à être envoyé, une fois les préférences utilisateur déjà
 * appliquées par {@link NotificationDispatcher}. {@code recipientUserIds} est vide pour les
 * événements sans compte utilisateur destinataire réel (absence/devoir : parents et élèves
 * n'ont pas de compte, voir ADR-010) — l'implémentation loggue alors simplement le contenu.
 */
public record NotificationEvent(NotificationType type, List<Long> recipientUserIds, String title, String body) {
}
