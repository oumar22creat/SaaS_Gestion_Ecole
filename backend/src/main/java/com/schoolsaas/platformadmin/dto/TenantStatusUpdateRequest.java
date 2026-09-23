package com.schoolsaas.platformadmin.dto;

import com.schoolsaas.tenant.TenantStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * @param reason motif de la décision, conservé dans le journal. Obligatoire pour une
 *               suspension : couper l'accès d'un établissement entier ne doit jamais être un
 *               geste anonyme dont personne ne retrouve la raison six mois plus tard.
 */
public record TenantStatusUpdateRequest(@NotNull TenantStatus status, @Size(max = 500) String reason) {
}
