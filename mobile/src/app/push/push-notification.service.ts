import { HttpClient } from '@angular/common/http';
import { Injectable, InjectionToken, inject, signal } from '@angular/core';
import { Capacitor, PluginListenerHandle } from '@capacitor/core';
import { FirebaseMessaging, TokenReceivedEvent } from '@capacitor-firebase/messaging';
import { firstValueFrom, timeout } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthTokenService } from '../auth/auth-token.service';

/**
 * Ce que le service attend de Firebase Messaging — le sous-ensemble réellement utilisé.
 *
 * <p>Un port plutôt que l'objet du greffon en direct : ce dernier est un `Proxy` Capacitor,
 * sur lequel on ne peut pas poser d'espion, et les cas qui comptent (permission refusée,
 * réseau coupé à la déconnexion) ne se vérifient pas sur un vrai téléphone à chaque build.
 */
export interface MessagingPort {
  checkPermissions(): Promise<{ receive: string }>;
  requestPermissions(): Promise<{ receive: string }>;
  getToken(): Promise<{ token: string }>;
  deleteToken(): Promise<void>;
  addListener(
    eventName: 'tokenReceived',
    listener: (event: TokenReceivedEvent) => void,
  ): Promise<PluginListenerHandle>;
}

/** `null` hors d'un vrai téléphone : le web n'a pas de jeton d'appareil. */
export const PUSH_MESSAGING = new InjectionToken<MessagingPort | null>('PUSH_MESSAGING', {
  providedIn: 'root',
  factory: () =>
    Capacitor.isNativePlatform() && Capacitor.isPluginAvailable('FirebaseMessaging')
      ? (FirebaseMessaging as unknown as MessagingPort)
      : null,
});

/**
 * Attente maximale du retrait côté serveur à la déconnexion — reprise en cours, lecture du
 * jeton et requête DELETE comprises. Au-delà, on abandonne : un enseignant hors couverture
 * doit pouvoir se déconnecter, et le jeton est de toute façon invalidé chez Firebase juste
 * après (voir {@link PushNotificationService#retirerAvantDeconnexion}).
 */
export const PUSH_DELAI_MAX_MS = new InjectionToken<number>('PUSH_DELAI_MAX_MS', {
  providedIn: 'root',
  factory: () => 3000,
});

/**
 * Attente maximale de l'invalidation du jeton chez Firebase. Avec {@link PUSH_DELAI_MAX_MS},
 * borne la déconnexion à cinq secondes au total dans le pire cas.
 */
export const PUSH_DELAI_FIREBASE_MS = new InjectionToken<number>('PUSH_DELAI_FIREBASE_MS', {
  providedIn: 'root',
  factory: () => 2000,
});

/**
 * Enregistre l'appareil auprès du serveur pour recevoir les notifications push.
 *
 * <p>Passe par Firebase Messaging et non par {@code @capacitor/push-notifications}. Ce
 * dernier rend, sur iOS, le jeton APNs brut — soixante-quatre caractères — là où le serveur
 * envoie par Firebase, qui n'accepte que ses propres jetons. Le symptôme était muet :
 * l'appareil s'enregistrait, puis Firebase rejetait le jeton au premier envoi et la
 * passerelle le purgeait comme un appareil désinstallé.
 *
 * <p>Trois règles gouvernent ce service, et elles viennent du terrain plutôt que de la
 * technique.
 *
 * <p>La permission n'est demandée qu'<b>après</b> la connexion, jamais au premier
 * lancement. Un parent à qui l'on réclame l'autorisation de notifier avant même qu'il sache
 * ce que fait l'application refuse — et un refus ne se redemande pas : il faut aller le
 * changer dans les réglages du téléphone, ce que personne ne fait.
 *
 * <p>L'appareil est réenregistré à chaque lancement quand une session survit. La session est
 * gardée dans `localStorage` (voir AuthTokenService) : sans cette reprise, le jeton n'était
 * connu qu'en mémoire après une connexion, et une déconnexion faite après une simple
 * réouverture de l'application ne retirait rien.
 *
 * <p>Le jeton est retiré à la déconnexion, puis invalidé chez Firebase. Sur un téléphone
 * partagé — celui d'une école, celui d'un foyer — un enseignant se déconnecte, un parent se
 * connecte, et les notifications du premier ne doivent pas continuer d'arriver sur un écran
 * que lit désormais le second.
 */
@Injectable({ providedIn: 'root' })
export class PushNotificationService {
  private readonly http = inject(HttpClient);
  private readonly authTokenService = inject(AuthTokenService);
  private readonly messaging = inject(PUSH_MESSAGING);
  private readonly delaiMaxMs = inject(PUSH_DELAI_MAX_MS);
  private readonly delaiFirebaseMs = inject(PUSH_DELAI_FIREBASE_MS);

  /** Dernier jeton déclaré au serveur, pour savoir quoi retirer à la déconnexion. */
  private readonly jetonAppareil = signal<string | null>(null);
  private ecouteurPose = false;
  /** Enregistrement en cours, que la déconnexion attend pour retirer le bon jeton. */
  private enregistrementEnCours: Promise<void> | null = null;

  /**
   * Demande la permission, récupère le jeton et le déclare au serveur.
   *
   * <p>{@code getToken()} rend directement le jeton, là où l'ancien greffon le faisait
   * parvenir par un événement : plus d'écouteur à poser avant d'appeler l'enregistrement,
   * donc plus de course possible entre les deux.
   *
   * <p>Ne lève jamais : une notification est un confort, pas une condition d'usage. Un
   * téléphone sans services Google, une permission refusée, un réseau coupé — rien de cela
   * ne doit empêcher un enseignant de faire l'appel.
   */
  async enregistrerApresConnexion(): Promise<void> {
    await this.enregistrer(true);
  }

  /**
   * Reprise au lancement de l'application, quand une session a survécu à sa fermeture.
   *
   * <p>Même chemin qu'après la connexion, à une différence près : la permission n'est
   * jamais demandée ici. Si elle a été refusée, on n'insiste pas — la redemander à chaque
   * ouverture serait le meilleur moyen d'obtenir un refus définitif.
   */
  async reprendreAuDemarrage(): Promise<void> {
    if (!this.authTokenService.isAuthenticated()) {
      return;
    }
    await this.enregistrer(false);
  }

  /**
   * Retire l'appareil du compte courant, puis invalide son jeton chez Firebase.
   *
   * <p>À appeler <b>avant</b> d'effacer la session : la requête de retrait a besoin du jeton
   * d'authentification, sans lequel le serveur la refuse.
   *
   * <p>Le jeton est redemandé à Firebase s'il n'est pas en mémoire — c'est le cas après un
   * redémarrage de l'application, lorsque la reprise n'a pas abouti (hors ligne au
   * lancement). Un enregistrement encore en cours — reprise au démarrage sur réseau lent —
   * est attendu d'abord : sans cela, sa déclaration pourrait arriver au serveur après le
   * retrait. Le tout est borné par {@link PUSH_DELAI_MAX_MS}, puis l'invalidation par
   * {@link PUSH_DELAI_FIREBASE_MS} : hors couverture, la déconnexion se fait quand même.
   *
   * <p>{@code deleteToken()} vient en dernier et en filet de sécurité : même si le retrait
   * n'a pas atteint le serveur, le jeton qui y reste ne désigne plus rien. Firebase répondra
   * {@code UNREGISTERED} au prochain envoi et la passerelle oubliera la ligne. La connexion
   * suivante obtiendra un jeton neuf.
   */
  async retirerAvantDeconnexion(): Promise<void> {
    const messaging = this.messaging;
    if (!messaging) {
      return;
    }
    try {
      await avecDelai(
        this.retirerDuServeur(messaging, this.authTokenService.generation()),
        this.delaiMaxMs,
      );
    } catch (erreur) {
      console.warn("Retrait de l'appareil non transmis au serveur", erreur);
    }
    await this.invaliderLocalement();
  }

  /**
   * Invalide le jeton chez Firebase sans prévenir le serveur — pour une session déjà perdue
   * (rafraîchissement refusé), où aucune requête authentifiée n'est plus possible.
   */
  async invaliderLocalement(): Promise<void> {
    const messaging = this.messaging;
    this.jetonAppareil.set(null);
    if (!messaging) {
      return;
    }
    try {
      await avecDelai(messaging.deleteToken(), this.delaiFirebaseMs);
    } catch (erreur) {
      console.warn('Invalidation du jeton Firebase impossible', erreur);
    }
  }

  private enregistrer(demanderPermission: boolean): Promise<void> {
    const enregistrement = this.enregistrerSansSuivi(demanderPermission).finally(() => {
      if (this.enregistrementEnCours === enregistrement) {
        this.enregistrementEnCours = null;
      }
    });
    this.enregistrementEnCours = enregistrement;
    return enregistrement;
  }

  private async enregistrerSansSuivi(demanderPermission: boolean): Promise<void> {
    const messaging = this.messaging;
    if (!messaging) {
      return;
    }
    try {
      const permission = await messaging.checkPermissions();
      let accordee = permission.receive === 'granted';
      if (!accordee && demanderPermission) {
        accordee = (await messaging.requestPermissions()).receive === 'granted';
      }
      if (!accordee) {
        return;
      }

      const { token } = await messaging.getToken();
      await this.declarerAuServeur(token);
      await this.suivreLesRenouvellements(messaging);
    } catch (erreur) {
      // Journalisé et non avalé : sans cette trace, un push qui n'arrive jamais ne laisse
      // aucune piste — c'est ce qui nous a coûté plusieurs jours.
      console.error('Enregistrement push impossible', erreur);
    }
  }

  private async retirerDuServeur(messaging: MessagingPort, generation: number): Promise<void> {
    if (this.enregistrementEnCours) {
      await this.enregistrementEnCours;
    }
    /*
     * Sans condition sur la permission : sur Android, un jeton FCM existe même sans le droit
     * d'afficher, et l'utilisateur a pu retirer ce droit après s'être enregistré — la ligne
     * serveur, elle, est toujours là.
     */
    const jeton = this.jetonAppareil() ?? (await messaging.getToken()).token;
    /*
     * Abandonné en route (délai dépassé), ce retrait continue en arrière-plan. Si la session
     * a changé entre-temps — un autre compte s'est connecté —, la requête partirait avec son
     * jeton et lui retirerait l'appareil qu'il vient de déclarer.
     */
    if (!jeton || this.authTokenService.generation() !== generation) {
      return;
    }
    await firstValueFrom(
      this.http
        .delete(`${environment.apiUrl}/device-tokens/${encodeURIComponent(jeton)}`)
        .pipe(timeout(this.delaiMaxMs)),
    );
  }

  /**
   * Firebase renouvelle les jetons de lui-même — réinstallation, restauration de sauvegarde,
   * inactivité prolongée. Sans ce suivi, l'appareil deviendrait injoignable sans que rien ne
   * le signale, et seul un nouveau cycle connexion/déconnexion le rétablirait.
   */
  private async suivreLesRenouvellements(messaging: MessagingPort): Promise<void> {
    if (this.ecouteurPose) {
      return;
    }
    this.ecouteurPose = true;
    await messaging.addListener('tokenReceived', (evenement) => {
      // Hors session, `declarerAuServeur` ne déclare rien. `deleteToken()` à la déconnexion
      // pousse Firebase à émettre un jeton neuf : il appartiendra au prochain compte connecté.
      void this.declarerAuServeur(evenement.token);
    });
  }

  private async declarerAuServeur(jeton: string): Promise<void> {
    /*
     * Revérifié ici, juste avant d'envoyer : entre la lecture du jeton et cet appel, la
     * déconnexion a pu effacer la session. La déclaration partirait alors sans
     * authentification — ou, après la connexion d'un autre compte, au mauvais nom.
     */
    if (!this.authTokenService.isAuthenticated()) {
      return;
    }
    this.jetonAppareil.set(jeton);
    try {
      await firstValueFrom(
        this.http.post(`${environment.apiUrl}/device-tokens`, {
          token: jeton,
          platform: Capacitor.getPlatform() === 'ios' ? 'IOS' : 'ANDROID',
        }),
      );
    } catch (erreur) {
      console.error("Déclaration de l'appareil refusée par le serveur", erreur);
    }
  }
}

/** Rend `undefined` si la promesse n'a pas abouti dans le délai ; ne l'annule pas. */
async function avecDelai<T>(promesse: Promise<T>, delaiMs: number): Promise<T | undefined> {
  let minuteur: ReturnType<typeof setTimeout> | undefined;
  const expiration = new Promise<undefined>((resolve) => {
    minuteur = setTimeout(() => resolve(undefined), delaiMs);
  });
  try {
    return await Promise.race([promesse, expiration]);
  } finally {
    clearTimeout(minuteur);
  }
}
