package com.schoolsaas.auth;

/** Identité déduite d'un access token JWT valide, propagée via Spring Security. */
public record AuthenticatedPrincipal(
        Long subjectId,
        SubjectType subjectType,
        Long tenantId,
        String role,
        String email) {
}
