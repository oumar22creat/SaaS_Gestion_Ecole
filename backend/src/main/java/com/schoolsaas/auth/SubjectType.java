package com.schoolsaas.auth;

/** Distingue les deux tables de comptes possibles derrière un token JWT. */
public enum SubjectType {
    USER,
    PLATFORM_ADMIN
}
