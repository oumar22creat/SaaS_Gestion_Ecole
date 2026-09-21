import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';

export interface ReportCardEntry {
  subjectId: number;
  average: number | null;
  coefficient: number;
  teacherComment: string | null;
}

export interface ReportCard {
  id: number;
  studentId: number;
  schoolClassId: number;
  periodLabel: string;
  periodFrom: string;
  periodTo: string;
  generalAverage: number | null;
  generalComment: string | null;
  councilDecision: string | null;
  absenceCount: number;
  lateCount: number;
  entries: ReportCardEntry[];
}

export interface GenerateReportCardsRequest {
  schoolClassId: number;
  periodLabel: string;
  periodFrom: string;
  periodTo: string;
}

const BASE_URL = `${environment.apiUrl}/report-cards`;

@Injectable({ providedIn: 'root' })
export class ReportCardService {
  private readonly http = inject(HttpClient);

  async generate(request: GenerateReportCardsRequest): Promise<ReportCard[]> {
    const response = await firstValueFrom(
      this.http.post<ApiResponse<ReportCard[]>>(`${BASE_URL}/generate`, request),
    );
    return response.data;
  }

  async list(schoolClassId: number, periodLabel: string): Promise<ReportCard[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<ReportCard[]>>(BASE_URL, {
        params: { schoolClassId, periodLabel },
      }),
    );
    return response.data;
  }

  async update(id: number, generalComment: string, councilDecision: string): Promise<ReportCard> {
    const response = await firstValueFrom(
      this.http.put<ApiResponse<ReportCard>>(`${BASE_URL}/${id}`, {
        generalComment,
        councilDecision,
      }),
    );
    return response.data;
  }

  async downloadPdf(id: number): Promise<void> {
    const blob = await firstValueFrom(
      this.http.get(`${BASE_URL}/${id}/pdf`, { responseType: 'blob' }),
    );
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `bulletin-${id}.pdf`;
    anchor.click();
    URL.revokeObjectURL(url);
  }
}
