package com.schoolsaas.auth.dto;

import com.schoolsaas.auth.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Création d'un compte pour un membre du personnel de l'établissement courant
 * (cahier-des-charges.md §20.2). Le tenant n'est jamais transmis par le client : il vient du
 * contexte de requête, comme pour toute entité tenant-scopée.
 */
public record CreateUserRequest(
        @NotBlank @Email @Size(max = 255) String email,

        @NotBlank @Size(min = 8, max = 100, message = "8 caractères minimum") String password,

        @NotBlank @Size(max = 100) String firstName,

        @NotBlank @Size(max = 100) String lastName,

        @NotNull Role role) {
}
