package com.schoolsaas.auth;

/**
 * Rôles applicatifs scopés à un établissement (voir cahier-des-charges.md §5). Le rôle
 * Super-Administrateur n'en fait pas partie : il n'appartient à aucun tenant, voir
 * {@link PlatformAdmin} et docs/ARCHITECTURE.md ADR-008.
 *
 * <p>Simplification volontaire pour le MVP (ADR-008) : un rôle fixe par utilisateur plutôt
 * qu'un système de permissions dynamique table par table (`roles`/`permissions` du
 * DATA_MODEL.md) — le contrôle d'accès se fait par rôle via {@code @PreAuthorize} sur chaque
 * endpoint (voir CLAUDE.md règle 2 et docs/API_CONVENTIONS.md).
 */
public enum Role {
    ADMIN,
    DIRECTION,
    TEACHER,
    STUDENT,
    PARENT,
    VIE_SCOLAIRE,
    SECRETARY,
    ACCOUNTANT
}
