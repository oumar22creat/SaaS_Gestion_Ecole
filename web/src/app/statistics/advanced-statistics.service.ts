import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';

export interface EvolutionPoint {
  period: string;
  average: number;
  gradeCount: number;
}

export interface ResultsEvolution {
  schoolClassId: number;
  subjectId: number | null;
  points: EvolutionPoint[];
}

@Injectable({ providedIn: 'root' })
export class AdvancedStatisticsService {
  private readonly http = inject(HttpClient);

  async resultsEvolution(schoolClassId: number, subjectId?: number): Promise<ResultsEvolution> {
    const params: Record<string, string | number> = { schoolClassId };
    if (subjectId) {
      params['subjectId'] = subjectId;
    }
    const response = await firstValueFrom(
      this.http.get<ApiResponse<ResultsEvolution>>(`${environment.apiUrl}/statistics/advanced/results-evolution`, {
        params,
      }),
    );
    return response.data;
  }

  async downloadCsv(schoolClassId: number): Promise<void> {
    const csv = await firstValueFrom(
      this.http.get(`${environment.apiUrl}/statistics/advanced/results-evolution.csv`, {
        params: { schoolClassId },
        responseType: 'text',
      }),
    );
    const blob = new Blob([csv], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = 'resultats-evolution.csv';
    anchor.click();
    URL.revokeObjectURL(url);
  }
}
