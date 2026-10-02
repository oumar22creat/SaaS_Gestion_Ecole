package com.schoolsaas.notification;

/**
 * Plateforme d'un appareil enregistré. Conservée alors que FCM achemine indifféremment vers
 * les trois : elle sert au diagnostic (« les iPhone ne reçoivent rien » est la panne la plus
 * fréquente, APNs ayant ses propres causes de rejet) et permettra de moduler la charge utile,
 * iOS et Android ne traitant pas de la même façon une notification reçue en avant-plan.
 */
public enum DevicePlatform {
    ANDROID,
    IOS,
    WEB
}
