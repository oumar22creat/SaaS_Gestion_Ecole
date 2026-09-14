package com.schoolsaas.notification;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, Long> {

    Optional<NotificationPreference> findByUserIdAndType(Long userId, NotificationType type);

    List<NotificationPreference> findAllByUserId(Long userId);

    List<NotificationPreference> findAllByUserIdInAndType(List<Long> userIds, NotificationType type);
}
