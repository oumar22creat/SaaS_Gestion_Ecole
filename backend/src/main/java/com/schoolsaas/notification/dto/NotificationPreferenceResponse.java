package com.schoolsaas.notification.dto;

import com.schoolsaas.notification.NotificationType;

public record NotificationPreferenceResponse(NotificationType type, boolean enabled) {
}
