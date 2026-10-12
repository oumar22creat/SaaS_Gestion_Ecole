import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';

export type NotificationType =
  | 'NEW_GRADE'
  | 'ABSENCE'
  | 'NEW_HOMEWORK'
  | 'NEW_DOCUMENT'
  | 'NEW_MESSAGE'
  | 'ANNOUNCEMENT'
  | 'SUBSCRIPTION_ALERT'
  | 'PAYMENT_RECEIPT'
  | 'LIBRARY_OVERDUE';

export interface NotificationPreference {
  type: NotificationType;
  enabled: boolean;
}

/** Ce que chaque type recouvre, dit dans les mots d'une famille. */
export const NOTIFICATION_LABELS: Record<NotificationType, string> = {
  ABSENCE: 'Absences et retards',
  NEW_GRADE: 'Nouvelles notes',
  NEW_HOMEWORK: 'Devoirs à faire',
  NEW_DOCUMENT: 'Documents déposés',
  NEW_MESSAGE: 'Messages',
  ANNOUNCEMENT: "Annonces de l'établissement",
  PAYMENT_RECEIPT: 'Reçus de règlement',
  LIBRARY_OVERDUE: 'Livres à rendre',
  SUBSCRIPTION_ALERT: "Alertes d'abonnement",
};

const BASE_URL = `${environment.apiUrl}/notification-preferences`;

@Injectable({ providedIn: 'root' })
export class NotificationPreferenceService {
  private readonly http = inject(HttpClient);

  async list(): Promise<NotificationPreference[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<NotificationPreference[]>>(BASE_URL),
    );
    return response.data;
  }

  async update(type: NotificationType, enabled: boolean): Promise<void> {
    await firstValueFrom(this.http.put(`${BASE_URL}/${type}`, { enabled }));
  }
}
