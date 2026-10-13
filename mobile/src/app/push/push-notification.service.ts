import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { Capacitor } from '@capacitor/core';
import { PushNotifications } from '@capacitor/push-notifications';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';

/**
 * Enregistre l'appareil auprès du serveur pour recevoir les notifications push.
 *
 * <p>Deux règles gouvernent ce service, et elles viennent du terrain plutôt que de la
 * technique.
 *
 * <p>La permission n'est demandée qu'<b>après</b> la connexion, jamais au premier
 * lancement. Un parent à qui l'on réclame l'autorisation de notifier avant même qu'il sache
 * ce que fait l'application refuse — et un refus ne se redemande pas : il faut aller le
 * changer dans les réglages du téléphone, ce que personne ne fait. On demande donc au
 * moment où l'utilisateur a une raison de dire oui.
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
  private ecouteursPoses = false;

  /** Vrai seulement sur un vrai téléphone : le web n'a pas de jeton FCM. */
  private disponible(): boolean {
    return Capacitor.isNativePlatform() && Capacitor.isPluginAvailable('PushNotifications');
  }

  /**
   * Demande la permission, puis enregistre l'appareil. Appelé après une connexion réussie.
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
      const permission = await PushNotifications.checkPermissions();
      const accordee =
        permission.receive === 'granted'
          ? permission
          : await PushNotifications.requestPermissions();
      if (accordee.receive !== 'granted') {
        return;
      }
      await this.poserLesEcouteurs();
      await PushNotifications.register();
    } catch {
      // Silencieux à dessein : voir le contrat de la méthode.
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
   * Posés une seule fois, et <b>attendus</b> avant d'appeler register().
   *
   * <p>addListener rend une promesse : ne pas l'attendre ouvre une course avec
   * l'événement d'enregistrement, qui arrive parfois avant que l'écouteur soit en place.
   * Le jeton est alors émis dans le vide, et rien ne le signale — l'application a l'air
   * de fonctionner, simplement aucun appareil n'est jamais enregistré.
   */
  private async poserLesEcouteurs(): Promise<void> {
    if (this.ecouteursPoses) {
      return;
    }
    this.ecouteursPoses = true;

    await PushNotifications.addListener('registration', (jeton) => {
      this.jetonAppareil.set(jeton.value);
      void this.declarerAuServeur(jeton.value);
    });

    await PushNotifications.addListener('registrationError', (erreur) => {
      // Journalisé et non avalé : c'est le seul endroit où l'on apprend qu'APNs a refusé,
      // et sans cette trace un push qui n'arrive pas ne laisse aucune piste.
      console.error('Enregistrement push refusé par le système', erreur);
    });
  }

  private async declarerAuServeur(jeton: string): Promise<void> {
    try {
      await firstValueFrom(
        this.http.post(`${environment.apiUrl}/device-tokens`, {
          token: jeton,
          platform: Capacitor.getPlatform() === 'ios' ? 'IOS' : 'ANDROID',
        }),
      );
    } catch {
      // Sans conséquence immédiate : la prochaine connexion réessaiera.
    }
  }
}
