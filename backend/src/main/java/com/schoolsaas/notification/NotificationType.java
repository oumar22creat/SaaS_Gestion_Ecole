package com.schoolsaas.notification;

/**
 * Catégories d'événements notifiables (cahier-des-charges.md §16, ROADMAP.md 2.5). Liste
 * fermée volontairement : chaque nouveau type d'événement notifiable dans le produit doit
 * passer par ici plutôt que de recréer un gateway ad hoc (voir {@link NotificationDispatcher}).
 */
public enum NotificationType {
    NEW_GRADE,
    ABSENCE,
    NEW_HOMEWORK,
    NEW_DOCUMENT,
    NEW_MESSAGE,
    ANNOUNCEMENT,
    SUBSCRIPTION_ALERT,
    LIBRARY_OVERDUE
}
