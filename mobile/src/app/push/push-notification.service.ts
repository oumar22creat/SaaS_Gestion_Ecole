import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Capacitor } from '@capacitor/core';
import { FirebaseMessaging } from '@capacitor-firebase/messaging';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';

/**
 * Enregistre l'appareil auprès du serveur pour recevoir les notifications push.
 *
 * <p>Passe par Firebase Messaging et non par {@code @capacitor/push-notifications}. Ce
 * dernier rend, sur iOS, le jeton APNs brut — soixante-quatre caractères — là où le serveur
 * envoie par Firebase, qui n'accepte que ses propres jetons. Le symptôme était muet :
 * l'appareil s'enregistrait, puis Firebase rejetait le jeton au premier envoi et la
 * passerelle le purgeait comme un appareil désinstallé.
 *
 * <p>Deux règles gouvernent ce service, et elles viennent du terrain plutôt que de la
 * technique.
 *
 * <p>La permission n'est demandée qu'<b>après</b> la connexion, jamais au premier
 * lancement. Un parent à qui l'on réclame l'autorisation de notifier avant même qu'il sache
 * ce que fait l'application refuse — et un refus ne se redemande pas : il faut aller le
 * changer dans les réglages du téléphone, ce que personne ne fait.
 *
 * <p>Le jeton est retiré à la déconnexion. Sur un téléphone partagé — celui d'une école,
 * celui d'un foyer — un enseignant se déconnecte, un parent se connecte, et les
 * notifications du premier ne doivent pas continuer d'arriver sur un écran que lit
 * désormais le second.
 */
@Injectable({ providedIn: 'root' })
export class PushNotificationService {
  private readonly http = inject(HttpClient);

  /** Dernier jeton enregistré, pour savoir quoi retirer à la déconnexion. */
  private readonly jetonAppareil = signal<string | null>(null);
  private ecouteurPose = false;

  /** Vrai seulement sur un vrai téléphone : le web n'a pas de jeton d'appareil. */
  private disponible(): boolean {
    return Capacitor.isNativePlatform() && Capacitor.isPluginAvailable('FirebaseMessaging');
  }

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
    if (!this.disponible()) {
      return;
    }
    try {
      const permission = await FirebaseMessaging.checkPermissions();
      const accordee =
        permission.receive === 'granted'
          ? permission
          : await FirebaseMessaging.requestPermissions();
      if (accordee.receive !== 'granted') {
        return;
      }

      const { token } = await FirebaseMessaging.getToken();
      await this.declarerAuServeur(token);
      await this.suivreLesRenouvellements();
    } catch (erreur) {
      // Journalisé et non avalé : sans cette trace, un push qui n'arrive jamais ne laisse
      // aucune piste — c'est ce qui nous a coûté plusieurs jours.
      console.error("Enregistrement push impossible", erreur);
    }
  }

  /**
   * Retire l'appareil du compte courant. À appeler avant d'effacer les jetons de session,
   * sinon la requête part sans en-tête d'authentification et le serveur la refuse.
   */
  async retirerAvantDeconnexion(): Promise<void> {
    const jeton = this.jetonAppareil();
    if (!jeton) {
      return;
    }
    this.jetonAppareil.set(null);
    try {
      await firstValueFrom(
        this.http.delete(`${environment.apiUrl}/device-tokens/${encodeURIComponent(jeton)}`),
      );
    } catch {
      // Le serveur réaffecte de toute façon un jeton déjà connu au compte qui l'enregistre :
      // un retrait manqué ne laisse pas l'appareil rattaché à l'ancien compte très longtemps.
    }
  }

  /**
   * Firebase renouvelle les jetons de lui-même — réinstallation, restauration de sauvegarde,
   * inactivité prolongée. Sans ce suivi, l'appareil deviendrait injoignable sans que rien ne
   * le signale, et seul un nouveau cycle connexion/déconnexion le rétablirait.
   */
  private async suivreLesRenouvellements(): Promise<void> {
    if (this.ecouteurPose) {
      return;
    }
    this.ecouteurPose = true;
    await FirebaseMessaging.addListener('tokenReceived', (evenement) => {
      void this.declarerAuServeur(evenement.token);
    });
  }

  private async declarerAuServeur(jeton: string): Promise<void> {
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
