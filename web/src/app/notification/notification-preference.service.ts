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
  | 'LIBRARY_OVERDUE';

export interface NotificationPreference {
  type: NotificationType;
  enabled: boolean;
}

const TYPES: NotificationType[] = [
  'NEW_GRADE',
  'ABSENCE',
  'NEW_HOMEWORK',
  'NEW_DOCUMENT',
  'NEW_MESSAGE',
  'ANNOUNCEMENT',
  'SUBSCRIPTION_ALERT',
  'LIBRARY_OVERDUE',
];

@Injectable({ providedIn: 'root' })
export class NotificationPreferenceService {
  private readonly http = inject(HttpClient);
  readonly types = TYPES;

  async list(): Promise<NotificationPreference[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<NotificationPreference[]>>(`${environment.apiUrl}/notification-preferences`),
    );
    return response.data;
  }

  async update(type: NotificationType, enabled: boolean): Promise<void> {
    await firstValueFrom(this.http.put(`${environment.apiUrl}/notification-preferences/${type}`, { enabled }));
  }
}
