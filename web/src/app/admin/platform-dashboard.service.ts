import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';

export interface PlatformSummary {
  trialCount: number;
  activeCount: number;
  readOnlyCount: number;
  suspendedCount: number;
  cancelledCount: number;
  mrrCents: number;
  arrCents: number;
  currency: string;
  churnRate: number | null;
  conversionRate: number | null;
  notificationsSentCount: number;
}

@Injectable({ providedIn: 'root' })
export class PlatformDashboardService {
  private readonly http = inject(HttpClient);

  async summary(): Promise<PlatformSummary> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<PlatformSummary>>(`${environment.apiUrl}/admin/dashboard/summary`),
    );
    return response.data;
  }
}
