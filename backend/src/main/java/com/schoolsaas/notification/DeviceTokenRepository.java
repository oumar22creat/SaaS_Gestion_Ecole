package com.schoolsaas.notification;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    Optional<DeviceToken> findByToken(String token);

    /** Les appareils des destinataires d'un envoi — l'accès le plus fréquent. */
    List<DeviceToken> findAllByUserIdIn(Collection<Long> userIds);

    /**
     * Retrait à la déconnexion, borné au compte qui le demande. Le filtre tenant s'applique à
     * la lecture qui précède la suppression : une ligne d'un autre établissement est invisible.
     */
    void deleteByTokenAndUserId(String token, Long userId);

    /** Retire les appareils que FCM vient de déclarer inconnus (application désinstallée). */
    void deleteAllByTokenIn(Collection<String> tokens);
}
