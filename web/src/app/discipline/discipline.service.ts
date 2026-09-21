import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';

export interface Incident {
  id: number;
  schoolClassId: number;
  occurredAt: string;
  severity: string;
  description: string;
  reportedByUserId: number;
  studentIds: number[];
  createdAt: string;
}

@Injectable({ providedIn: 'root' })
export class DisciplineService {
  private readonly http = inject(HttpClient);

  async listIncidents(schoolClassId: number, from: string, to: string): Promise<Incident[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<Incident[]>>(`${environment.apiUrl}/discipline/incidents`, {
        params: { schoolClassId, from, to },
      }),
    );
    return response.data;
  }

  async createIncident(body: {
    schoolClassId: number;
    occurredAt: string;
    severity: string;
    description: string;
    studentIds: number[];
  }): Promise<Incident> {
    const response = await firstValueFrom(
      this.http.post<ApiResponse<Incident>>(`${environment.apiUrl}/discipline/incidents`, body),
    );
    return response.data;
  }
}
