package com.schoolsaas.auth.dto;

import com.schoolsaas.auth.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Requêtes d'administration d'un compte existant. Trois opérations distinctes plutôt qu'un
 * PATCH fourre-tout : chacune a ses propres garde-fous (voir UserService) et se lit sans
 * ambiguïté dans un journal d'audit.
 */
public final class UpdateUserRequest {

    private UpdateUserRequest() {
    }

    /** Activation ou désactivation d'un compte. Un compte inactif ne peut plus se connecter. */
    public record Status(@NotNull Boolean active) {
    }

    /** Changement de rôle. Le rôle décide des écrans accessibles. */
    public record RoleChange(@NotNull Role role) {
    }

    /** Réinitialisation du mot de passe par un administrateur. */
    public record PasswordReset(
            @NotBlank @Size(min = 8, max = 100, message = "8 caractères minimum") String password) {
    }
}
