import { Component, inject, signal } from '@angular/core';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatIconModule } from '@angular/material/icon';
import { NotificationPreferenceService, NotificationType } from './notification-preference.service';

@Component({
  selector: 'app-notification-page',
  imports: [MatSlideToggleModule, MatIconModule],
  template: `
    <div class="page-header">
      <h1><mat-icon class="page-icon" aria-hidden="true">notifications</mat-icon>Notifications</h1>
    </div>
    <p>Choisissez les alertes que vous souhaitez recevoir.</p>
    @for (type of types; track type) {
      <p>
        <mat-slide-toggle [checked]="enabled(type)" (change)="toggle(type, $event.checked)">
          {{ label(type) }}
        </mat-slide-toggle>
      </p>
    }
  `,
})
export class NotificationPage {
  private readonly preferenceService = inject(NotificationPreferenceService);
  protected readonly types = this.preferenceService.types;
  private readonly prefs = signal<Record<string, boolean>>({});

  constructor() {
    void this.preferenceService.list().then((items) => {
      const map: Record<string, boolean> = {};
      for (const item of items) {
        map[item.type] = item.enabled;
      }
      this.prefs.set(map);
    });
  }

  enabled(type: NotificationType): boolean {
    return this.prefs()[type] !== false;
  }

  async toggle(type: NotificationType, enabled: boolean): Promise<void> {
    await this.preferenceService.update(type, enabled);
    this.prefs.update((current) => ({ ...current, [type]: enabled }));
  }

  label(type: NotificationType): string {
    const labels: Record<NotificationType, string> = {
      NEW_GRADE: 'Nouvelle note',
      ABSENCE: 'Absence',
      NEW_HOMEWORK: 'Nouveau devoir',
      NEW_DOCUMENT: 'Nouveau document',
      NEW_MESSAGE: 'Message',
      ANNOUNCEMENT: 'Annonce',
      SUBSCRIPTION_ALERT: 'Abonnement',
      LIBRARY_OVERDUE: 'Retard bibliothèque',
    };
    return labels[type];
  }
}
