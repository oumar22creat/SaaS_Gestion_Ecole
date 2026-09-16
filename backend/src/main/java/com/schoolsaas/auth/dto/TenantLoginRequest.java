package com.schoolsaas.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Connexion d'un compte d'établissement — distinct de {@link LoginRequest} (Super-Admin, hors
 * tenant) car il faut identifier l'établissement AVANT de chercher l'utilisateur par e-mail :
 * l'e-mail n'est unique que par établissement (cahier §21, {@code UNIQUE(school_id, email)}),
 * pas globalement, et la table {@code users} impose Row-Level Security — voir
 * docs/ARCHITECTURE.md ADR-029.
 */
public record TenantLoginRequest(
        @NotBlank String subdomain,
        @NotBlank @Email String email,
        @NotBlank String password) {
}
