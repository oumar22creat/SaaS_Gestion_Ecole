package com.schoolsaas.parent.dto;

import com.schoolsaas.parent.Parent;

/**
 * @param email       e-mail de contact du parent, saisi sur sa fiche
 * @param portalEmail adresse de connexion au portail mobile, ou null si aucun accès n'est
 *                    ouvert. Distincte du champ ci-dessus : rien n'oblige à avoir ouvert
 *                    l'accès avec l'adresse de contact, et c'est celle-ci qu'il faut dicter à
 *                    une famille qui a perdu son mot de passe.
 */
public record ParentResponse(
        Long id, String firstName, String lastName, String email, String phone, String portalEmail) {

    public static ParentResponse from(Parent parent, String portalEmail) {
        return new ParentResponse(
                parent.getId(),
                parent.getFirstName(),
                parent.getLastName(),
                parent.getEmail(),
                parent.getPhone(),
                portalEmail);
    }
}
