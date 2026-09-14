package com.schoolsaas.document;

/**
 * Frontière avec le stockage objet (cahier-des-charges.md §14 : "S3-compatible avec quota
 * par tenant"). Implémentation par défaut sur disque local (voir
 * {@link LocalDiskStorageGateway}) — S3 réel non câblé (pas de credentials), même pattern
 * que {@code StripeGateway}/{@code ParentNotificationGateway} : brancher S3 reste un
 * remplacement d'implémentation, pas un changement d'appelants. Voir docs/ARCHITECTURE.md
 * ADR-017 pour le détail (quota par tenant notamment hors périmètre : le modèle `Plan`
 * n'a pas encore de champ de quota de stockage).
 */
public interface StorageGateway {

    /** @return une clé opaque permettant de retrouver le fichier plus tard. */
    String store(byte[] content, String fileName);

    byte[] retrieve(String storageKey);

    void delete(String storageKey);
}
