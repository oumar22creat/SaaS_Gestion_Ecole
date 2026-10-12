import { Component, inject, signal } from '@angular/core';
import {
  IonBackButton,
  IonButtons,
  IonContent,
  IonHeader,
  IonItem,
  IonLabel,
  IonList,
  IonListHeader,
  IonNote,
  IonTitle,
  IonToggle,
  IonToolbar,
} from '@ionic/angular';
import { extractErrorMessage } from '../core/http-error.util';
import {
  NOTIFICATION_LABELS,
  NotificationPreference,
  NotificationPreferenceService,
  NotificationType,
} from './notification-preference.service';

/**
 * Réglage des notifications, côté mobile.
 *
 * <p>Ce réglage n'existait que sur le web, et c'était le défaut qui comptait : un parent
 * qui ne consulte que son téléphone ne pouvait pas couper ce qui l'importune. Il coupait
 * alors tout, au niveau du système — et une permission refusée ne se redemande pas, elle
 * se change dans les réglages du téléphone, ce que personne ne fait. L'établissement
 * perdait le canal entier faute d'un interrupteur sur une ligne.
 */
@Component({
  selector: 'app-notification-preferences-page',
  imports: [
    IonHeader, IonToolbar, IonTitle, IonButtons, IonBackButton, IonContent,
    IonList, IonListHeader, IonItem, IonLabel, IonToggle, IonNote,
  ],
  template: `
    <ion-header>
      <ion-toolbar>
        <ion-buttons slot="start"><ion-back-button defaultHref="/home" /></ion-buttons>
        <ion-title>Notifications</ion-title>
      </ion-toolbar>
    </ion-header>
    <ion-content class="ion-padding">
      <p class="intro">
        Choisissez ce que vous souhaitez recevoir. Couper une ligne ici vaut mieux que
        désactiver toutes les notifications depuis les réglages du téléphone : vous
        continuerez à recevoir ce qui compte.
      </p>

      @if (errorMessage()) {
        <p class="erreur">{{ errorMessage() }}</p>
      }

      @if (loading()) {
        <p class="intro">Chargement…</p>
      } @else {
        <ion-list>
          <ion-list-header><ion-label>Ce que vous recevez</ion-label></ion-list-header>
          @for (preference of preferences(); track preference.type) {
            <ion-item>
              <ion-label>{{ label(preference.type) }}</ion-label>
              <ion-toggle
                slot="end"
                [checked]="preference.enabled"
                (ionChange)="toggle(preference, $any($event).detail.checked)"
              />
            </ion-item>
          }
        </ion-list>
        <ion-note class="note">
          Les absences de votre enfant restent envoyées par SMS si l'établissement l'a
          activé, même si vous coupez la ligne ici.
        </ion-note>
      }
    </ion-content>
  `,
  styles: `
    .intro {
      margin: 0 0 1rem;
      color: var(--ion-color-medium);
      line-height: 1.5;
    }

    .erreur {
      color: var(--ion-color-danger);
    }

    .note {
      display: block;
      margin-top: 1rem;
      line-height: 1.4;
    }
  `,
})
export class NotificationPreferencesPage {
  private readonly service = inject(NotificationPreferenceService);

  protected readonly preferences = signal<NotificationPreference[]>([]);
  protected readonly loading = signal(true);
  protected readonly errorMessage = signal<string | null>(null);

  constructor() {
    void this.load();
  }

  protected label(type: NotificationType): string {
    return NOTIFICATION_LABELS[type] ?? type;
  }

  private async load(): Promise<void> {
    try {
      this.preferences.set(await this.service.list());
    } catch (error) {
      this.errorMessage.set(extractErrorMessage(error));
    } finally {
      this.loading.set(false);
    }
  }

  /**
   * L'interrupteur bascule immédiatement, et revient en arrière si le serveur refuse. Une
   * bascule qui attend la réponse réseau donne l'impression d'un écran figé sur une
   * connexion lente, ce qui est la règle ici.
   */
  protected async toggle(preference: NotificationPreference, enabled: boolean): Promise<void> {
    if (preference.enabled === enabled) {
      return;
    }
    this.appliquer(preference.type, enabled);
    this.errorMessage.set(null);
    try {
      await this.service.update(preference.type, enabled);
    } catch (error) {
      this.appliquer(preference.type, !enabled);
      this.errorMessage.set(extractErrorMessage(error));
    }
  }

  private appliquer(type: NotificationType, enabled: boolean): void {
    this.preferences.update((liste) =>
      liste.map((item) => (item.type === type ? { ...item, enabled } : item)),
    );
  }
}
