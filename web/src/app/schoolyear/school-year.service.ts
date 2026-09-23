import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';

const BASE_URL = `${environment.apiUrl}/school-years`;

export type SchoolYearStatus = 'PLANNED' | 'ACTIVE' | 'CLOSED';

export interface SchoolYear {
  id: number;
  label: string;
  startDate: string;
  endDate: string;
  status: SchoolYearStatus;
  enrolledCount: number;
}

/** Devenir d'un élève en fin d'année. */
export type EnrollmentOutcome = 'PROMOTED' | 'REPEATING' | 'TRANSFERRED' | 'GRADUATED' | 'WITHDRAWN';

export interface PromotionDecision {
  studentId: number;
  outcome: EnrollmentOutcome;
  targetClassId: number | null;
}

export interface PromotionResult {
  targetYearId: number;
  targetYearLabel: string;
  promotedCount: number;
  repeatingCount: number;
  leftCount: number;
  /** Ce qui n'a pas pu être réinscrit, rapporté plutôt qu'ignoré. */
  skipped: string[];
}

export interface Enrollment {
  id: number;
  schoolYearId: number;
  schoolYearLabel: string;
  studentId: number;
  studentName: string;
  studentNumber: string;
  schoolClassId: number;
  className: string;
  status: string;
  enrolledAt: string;
  leftAt: string | null;
}

@Injectable({ providedIn: 'root' })
export class SchoolYearService {
  private readonly http = inject(HttpClient);

  async list(): Promise<SchoolYear[]> {
    const response = await firstValueFrom(this.http.get<ApiResponse<SchoolYear[]>>(BASE_URL));
    return response.data;
  }

  async create(label: string, startDate: string, endDate: string): Promise<SchoolYear> {
    const response = await firstValueFrom(
      this.http.post<ApiResponse<SchoolYear>>(BASE_URL, { label, startDate, endDate }),
    );
    return response.data;
  }

  async activate(id: number): Promise<SchoolYear> {
    const response = await firstValueFrom(
      this.http.put<ApiResponse<SchoolYear>>(`${BASE_URL}/${id}/activate`, {}),
    );
    return response.data;
  }

  async close(id: number): Promise<SchoolYear> {
    const response = await firstValueFrom(
      this.http.put<ApiResponse<SchoolYear>>(`${BASE_URL}/${id}/close`, {}),
    );
    return response.data;
  }

  async promote(
    targetYearId: number,
    sourceYearId: number,
    decisions: PromotionDecision[],
  ): Promise<PromotionResult> {
    const response = await firstValueFrom(
      this.http.post<ApiResponse<PromotionResult>>(`${BASE_URL}/${targetYearId}/promotions`, {
        sourceYearId,
        decisions,
      }),
    );
    return response.data;
  }

  async historyForStudent(studentId: number): Promise<Enrollment[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<Enrollment[]>>(`${BASE_URL}/enrollments`, {
        params: { studentId: String(studentId) },
      }),
    );
    return response.data;
  }
}
