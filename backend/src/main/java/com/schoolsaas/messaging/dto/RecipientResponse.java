package com.schoolsaas.messaging.dto;

import com.schoolsaas.auth.User;

/**
 * Entrée de l'annuaire de messagerie (ADR-019) : de quoi choisir un destinataire, rien de
 * plus. Volontairement SANS e-mail ni téléphone — l'annuaire est ouvert à tout le personnel,
 * pas seulement à l'administration qui gère les comptes.
 */
public record RecipientResponse(Long id, String firstName, String lastName, String displayName, String role) {

    public static RecipientResponse from(User user) {
        return new RecipientResponse(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                (user.getFirstName() + " " + user.getLastName()).trim(),
                user.getRole().name());
    }
}
