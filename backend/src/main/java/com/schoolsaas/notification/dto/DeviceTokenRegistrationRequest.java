package com.schoolsaas.notification.dto;

import com.schoolsaas.notification.DevicePlatform;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Enregistrement d'un appareil par l'application cliente (docs/NOTIFICATIONS-PUSH.md). */
public record DeviceTokenRegistrationRequest(
        @NotBlank @Size(max = 4096) String token, @NotNull DevicePlatform platform) {
}
