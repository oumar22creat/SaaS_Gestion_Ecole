package com.schoolsaas.notification;

import com.schoolsaas.auth.AuthenticatedPrincipal;
import com.schoolsaas.common.ApiResponse;
import com.schoolsaas.notification.dto.NotificationPreferenceResponse;
import com.schoolsaas.notification.dto.NotificationPreferenceUpdateRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Paramétrage des notifications par l'utilisateur courant (cahier-des-charges.md §16). */
@RestController
@RequestMapping("/api/v1/notification-preferences")
public class NotificationPreferenceController {

    private final NotificationPreferenceService preferenceService;

    public NotificationPreferenceController(NotificationPreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping
    public ApiResponse<List<NotificationPreferenceResponse>> list() {
        return ApiResponse.of(preferenceService.listForUser(currentUserId()));
    }

    @PutMapping("/{type}")
    public ApiResponse<NotificationPreferenceResponse> update(
            @PathVariable NotificationType type, @Valid @RequestBody NotificationPreferenceUpdateRequest request) {
        preferenceService.update(currentUserId(), type, request.enabled());
        return ApiResponse.of(new NotificationPreferenceResponse(type, request.enabled()));
    }

    private Long currentUserId() {
        return ((AuthenticatedPrincipal) SecurityContextHolder.getContext().getAuthentication().getPrincipal()).subjectId();
    }
}
