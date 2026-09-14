package com.schoolsaas.notification.dto;

import jakarta.validation.constraints.NotNull;

public record NotificationPreferenceUpdateRequest(@NotNull Boolean enabled) {
}
