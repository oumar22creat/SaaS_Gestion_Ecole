package com.schoolsaas.notification;

import java.util.Collection;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Enregistrement des appareils joignables par notification (docs/NOTIFICATIONS-PUSH.md).
 *
 * <p>L'enregistrement est idempotent : l'application le rejoue à chaque lancement, car FCM
 * renouvelle les jetons sans prévenir — à la réinstallation, à la restauration d'une
 * sauvegarde, parfois spontanément. Un client qui n'enregistrerait qu'une fois deviendrait
 * silencieusement injoignable.
 */
@Service
public class DeviceTokenService {

    private final DeviceTokenRepository deviceTokenRepository;

    public DeviceTokenService(DeviceTokenRepository deviceTokenRepository) {
        this.deviceTokenRepository = deviceTokenRepository;
    }

    /**
     * Rattache l'appareil au compte indiqué, en réattribuant le jeton s'il était déjà connu.
     *
     * <p>La réattribution est le cas qui compte : sur un téléphone partagé, le compte suivant
     * ne doit pas hériter des notifications du précédent.
     */
    @Transactional
    public void enregistrer(Long userId, String token, DevicePlatform platform) {
        try {
            appliquer(userId, token, platform);
        } catch (DataIntegrityViolationException concurrence) {
            // Deux enregistrements simultanés du même appareil — l'application rejoue à chaque
            // lancement, et deux onglets ou un redémarrage rapide suffisent à les faire se
            // croiser. La ligne existe désormais : on la met à jour au lieu d'échouer.
            appliquer(userId, token, platform);
        }
    }

    private void appliquer(Long userId, String token, DevicePlatform platform) {
        deviceTokenRepository
                .findByToken(token)
                .ifPresentOrElse(
                        connu -> connu.reattribuerA(userId, platform),
                        () -> deviceTokenRepository.save(new DeviceToken(userId, token, platform)));
    }

    /**
     * Retire l'appareil, à la déconnexion.
     *
     * <p>Sans cela, le téléphone d'un enseignant qui quitte l'établissement continuerait de
     * recevoir les absences de ses anciennes classes jusqu'à ce que FCM le déclare inconnu —
     * ce qui n'arrive jamais tant que l'application reste installée.
     */
    @Transactional
    public void retirer(String token) {
        deviceTokenRepository.deleteByToken(token);
    }

    /** Les appareils à joindre pour ces destinataires. */
    @Transactional(readOnly = true)
    public List<DeviceToken> appareilsDe(Collection<Long> userIds) {
        return userIds.isEmpty() ? List.of() : deviceTokenRepository.findAllByUserIdIn(userIds);
    }

    /** Oublie les appareils que FCM vient de déclarer inconnus (application désinstallée). */
    @Transactional
    public void oublier(Collection<String> tokens) {
        if (!tokens.isEmpty()) {
            deviceTokenRepository.deleteAllByTokenIn(tokens);
        }
    }
}
