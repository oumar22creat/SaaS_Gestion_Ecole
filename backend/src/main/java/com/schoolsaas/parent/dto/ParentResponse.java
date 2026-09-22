package com.schoolsaas.parent.dto;

import com.schoolsaas.parent.Parent;

/**
 * @param hasPortalAccess vrai si un compte de connexion au portail est ouvert. Booléen et non
 *                        l'identifiant du compte : l'écran a besoin de savoir si l'accès
 *                        existe, pas de le désigner.
 */
public record ParentResponse(
        Long id, String firstName, String lastName, String email, String phone, boolean hasPortalAccess) {

    public static ParentResponse from(Parent parent) {
        return new ParentResponse(
                parent.getId(),
                parent.getFirstName(),
                parent.getLastName(),
                parent.getEmail(),
                parent.getPhone(),
                parent.getUserId() != null);
    }
}
