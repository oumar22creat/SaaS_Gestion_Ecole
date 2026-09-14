import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';
import { DashboardSummary } from './dashboard.model';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);

  async summary(): Promise<DashboardSummary> {
    const response = await firstValueFrom(this.http.get<ApiResponse<DashboardSummary>>(`${environment.apiUrl}/dashboard/summary`));
    return response.data;
  }
}
