package com.schoolsaas.platformadmin.dto;

import com.schoolsaas.auth.PlatformAdmin;
import java.time.Instant;

public record PlatformAdminResponse(Long id, String email, String firstName, String lastName, Instant createdAt) {

    public static PlatformAdminResponse from(PlatformAdmin admin) {
        return new PlatformAdminResponse(
                admin.getId(), admin.getEmail(), admin.getFirstName(), admin.getLastName(), admin.getCreatedAt());
    }
}
