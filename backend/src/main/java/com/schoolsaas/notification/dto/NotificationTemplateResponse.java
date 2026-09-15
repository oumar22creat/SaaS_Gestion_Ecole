package com.schoolsaas.notification.dto;

import com.schoolsaas.notification.NotificationType;

public record NotificationTemplateResponse(NotificationType type, String titleOverride, String bodyTemplate) {
}
