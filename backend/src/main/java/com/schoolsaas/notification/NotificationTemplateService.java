package com.schoolsaas.notification;

import com.schoolsaas.common.ApiException;
import com.schoolsaas.notification.dto.NotificationTemplateResponse;
import com.schoolsaas.notification.dto.NotificationTemplateUpdateRequest;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Templates de notification/e-mail personnalisables par établissement (cahier §2.4, ROADMAP.md 3.7). */
@Service
public class NotificationTemplateService {

    private final NotificationTemplateRepository templateRepository;

    public NotificationTemplateService(NotificationTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    public List<NotificationTemplateResponse> listForCurrentTenant() {
        Map<NotificationType, NotificationTemplate> existing = templateRepository.findAll().stream()
                .collect(Collectors.toMap(NotificationTemplate::getType, Function.identity()));
        return Arrays.stream(NotificationType.values())
                .map(type -> toResponse(type, existing.get(type)))
                .toList();
    }

    @Transactional
    public NotificationTemplateResponse update(NotificationType type, NotificationTemplateUpdateRequest request) {
        validate(request);
        NotificationTemplate template = templateRepository.findByType(type)
                .orElseGet(() -> templateRepository.save(new NotificationTemplate(type)));
        template.setTitleOverride(blankToNull(request.titleOverride()));
        template.setBodyTemplate(blankToNull(request.bodyTemplate()));
        return toResponse(type, template);
    }

    private void validate(NotificationTemplateUpdateRequest request) {
        String bodyTemplate = request.bodyTemplate();
        if (bodyTemplate != null && !bodyTemplate.isBlank() && !bodyTemplate.contains(NotificationTemplate.MESSAGE_PLACEHOLDER)) {
            throw ApiException.badRequest(
                    "INVALID_NOTIFICATION_TEMPLATE",
                    "Le corps personnalisé doit contenir le placeholder " + NotificationTemplate.MESSAGE_PLACEHOLDER,
                    List.of());
        }
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private NotificationTemplateResponse toResponse(NotificationType type, NotificationTemplate template) {
        if (template == null) {
            return new NotificationTemplateResponse(type, null, null);
        }
        return new NotificationTemplateResponse(type, template.getTitleOverride(), template.getBodyTemplate());
    }
}
