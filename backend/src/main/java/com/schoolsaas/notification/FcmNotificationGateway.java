package com.schoolsaas.notification;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.SendResponse;
import jakarta.annotation.PostConstruct;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * Envoi effectif des notifications par Firebase Cloud Messaging (docs/NOTIFICATIONS-PUSH.md).
 *
 * <p>Active seulement si {@code app.fcm.enabled} vaut vrai ; sinon {@link
 * LoggingNotificationGateway} tient la place. C'est le remplacement d'implémentation annoncé
 * par docs/ARCHITECTURE.md ADR-020 : aucun appelant ne change.
 */
@Component
@ConditionalOnProperty(prefix = "app.fcm", name = "enabled", havingValue = "true")
public class FcmNotificationGateway implements NotificationGateway {

    private static final Logger log = LoggerFactory.getLogger(FcmNotificationGateway.class);

    /** Limite de l'API : au-delà, FCM refuse le lot entier. */
    private static final int TAILLE_LOT = 500;

    private final FcmProperties properties;
    private final DeviceTokenService deviceTokenService;

    public FcmNotificationGateway(FcmProperties properties, DeviceTokenService deviceTokenService) {
        this.properties = properties;
        this.deviceTokenService = deviceTokenService;
    }

    /**
     * Ouvre la connexion au démarrage plutôt qu'au premier envoi.
     *
     * <p>Un chemin erroné ou une clé révoquée se découvre ainsi au déploiement, dans les
     * journaux de démarrage, et non des semaines plus tard lors de la première absence
     * signalée — moment où personne ne fait le lien.
     */
    @PostConstruct
    void initialiser() throws IOException {
        if (!FirebaseApp.getApps().isEmpty()) {
            return;
        }
        try (FileInputStream cle = new FileInputStream(properties.credentialsPath())) {
            FirebaseApp.initializeApp(FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.fromStream(cle))
                    .build());
        }
        log.info("Notifications push activées (compte de service : {})", properties.credentialsPath());
    }

    @Override
    public void send(NotificationEvent event) {
        List<DeviceToken> appareils = deviceTokenService.appareilsDe(event.recipientUserIds());
        if (appareils.isEmpty()) {
            // Cas ordinaire, pas une anomalie : une famille sans compte, ou qui n'a pas encore
            // installé l'application. Le SMS reste son canal (voir SmsService).
            log.debug("Notification {} : aucun appareil enregistré pour {} destinataire(s)",
                    event.type(), event.recipientUserIds().size());
            return;
        }
        List<String> jetons = appareils.stream().map(DeviceToken::getToken).toList();
        for (int debut = 0; debut < jetons.size(); debut += TAILLE_LOT) {
            envoyerUnLot(event, jetons.subList(debut, Math.min(debut + TAILLE_LOT, jetons.size())));
        }
    }

    private void envoyerUnLot(NotificationEvent event, List<String> jetons) {
        MulticastMessage message = MulticastMessage.builder()
                .setNotification(Notification.builder()
                        .setTitle(event.title())
                        .setBody(event.body())
                        .build())
                // Le type voyage en donnée : il permet à l'application d'ouvrir le bon écran
                // quand l'utilisateur touche la notification.
                .putData("type", event.type().name())
                .addAllTokens(jetons)
                .build();
        try {
            BatchResponse reponse = FirebaseMessaging.getInstance().sendEachForMulticast(message);
            oublierLesAppareilsDisparus(reponse, jetons);
        } catch (Exception e) {
            // Une notification perdue ne doit jamais faire échouer l'opération métier qui l'a
            // déclenchée : un appel se termine, une note s'enregistre, même si FCM est
            // injoignable. Même parti pris que SmsService.
            log.warn("Envoi push {} en échec pour {} appareil(s) : {}", event.type(), jetons.size(), e.getMessage());
        }
    }

    /**
     * Retire les appareils que FCM déclare inconnus.
     *
     * <p>Un jeton devient invalide quand l'application est désinstallée, et FCM ne le signale
     * qu'à la tentative d'envoi suivante. Sans cette purge, la table ne ferait que croître et
     * chaque envoi traînerait des adresses mortes.
     */
    private void oublierLesAppareilsDisparus(BatchResponse reponse, List<String> jetons) {
        List<String> disparus = new ArrayList<>();
        List<SendResponse> resultats = reponse.getResponses();
        for (int i = 0; i < resultats.size(); i++) {
            SendResponse resultat = resultats.get(i);
            if (resultat.isSuccessful()) {
                continue;
            }
            MessagingErrorCode code = resultat.getException() == null
                    ? null
                    : resultat.getException().getMessagingErrorCode();
            if (code == MessagingErrorCode.UNREGISTERED || code == MessagingErrorCode.INVALID_ARGUMENT) {
                disparus.add(jetons.get(i));
            }
        }
        if (!disparus.isEmpty()) {
            deviceTokenService.oublier(disparus);
            log.info("{} appareil(s) oublié(s) : application désinstallée ou jeton invalide", disparus.size());
        }
    }
}
