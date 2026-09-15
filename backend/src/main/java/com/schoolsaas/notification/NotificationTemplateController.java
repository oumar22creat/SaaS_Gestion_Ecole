package com.schoolsaas.notification;

import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.notification.dto.NotificationTemplateResponse;
import com.schoolsaas.notification.dto.NotificationTemplateUpdateRequest;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Templates de notification/e-mail personnalisables par établissement (cahier §2.4, ROADMAP.md 3.7). */
@RestController
@RequestMapping("/api/v1/tenants/current/notification-templates")
@PreAuthorize("hasAnyRole('ADMIN', 'DIRECTION')")
public class NotificationTemplateController {

    private final NotificationTemplateService templateService;

    public NotificationTemplateController(NotificationTemplateService templateService) {
        this.templateService = templateService;
    }

    @GetMapping
    public ApiResponse<List<NotificationTemplateResponse>> list() {
        return ApiResponse.of(templateService.listForCurrentTenant());
    }

    @PutMapping("/{type}")
    public ApiResponse<NotificationTemplateResponse> update(
            @PathVariable NotificationType type, @RequestBody NotificationTemplateUpdateRequest request) {
        return ApiResponse.of(templateService.update(type, request));
    }
}
