package com.schoolsaas.notification;

import com.schoolsaas.notification.dto.NotificationPreferenceResponse;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Paramétrage des notifications par utilisateur (cahier-des-charges.md §16). */
@Service
public class NotificationPreferenceService {

    private final NotificationPreferenceRepository preferenceRepository;

    public NotificationPreferenceService(NotificationPreferenceRepository preferenceRepository) {
        this.preferenceRepository = preferenceRepository;
    }

    public List<NotificationPreferenceResponse> listForUser(Long userId) {
        Map<NotificationType, Boolean> existing = preferenceRepository.findAllByUserId(userId).stream()
                .collect(Collectors.toMap(NotificationPreference::getType, NotificationPreference::isEnabled));
        return Arrays.stream(NotificationType.values())
                .map(type -> new NotificationPreferenceResponse(type, existing.getOrDefault(type, true)))
                .toList();
    }

    @Transactional
    public void update(Long userId, NotificationType type, boolean enabled) {
        NotificationPreference preference = preferenceRepository.findByUserIdAndType(userId, type)
                .orElseGet(() -> preferenceRepository.save(new NotificationPreference(userId, type, enabled)));
        preference.setEnabled(enabled);
    }
}
