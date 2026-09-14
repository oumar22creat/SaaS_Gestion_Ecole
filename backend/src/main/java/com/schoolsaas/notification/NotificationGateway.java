package com.schoolsaas.notification;

/**
 * Envoi effectif d'une notification (Firebase Cloud Messaging à terme, voir
 * docs/ARCHITECTURE.md ADR-020). Frontière volontairement séparée de
 * {@link NotificationDispatcher} — même pattern que {@code StripeGateway} : brancher FCM
 * reste un remplacement d'implémentation, pas un changement d'appelants. Voir
 * {@link LoggingNotificationGateway} pour l'implémentation par défaut (aucun credential FCM
 * disponible dans cet environnement).
 */
public interface NotificationGateway {

    void send(NotificationEvent event);
}
