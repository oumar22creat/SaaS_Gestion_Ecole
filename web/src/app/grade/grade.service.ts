import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';
import { GradeEntry, Grade } from './grade.model';

const BASE_URL = environment.apiUrl;

@Injectable({ providedIn: 'root' })
export class GradeService {
  private readonly http = inject(HttpClient);

  async listForExam(examId: number): Promise<Grade[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<Grade[]>>(`${BASE_URL}/exams/${examId}/grades`),
    );
    return response.data;
  }

  async submit(examId: number, entries: GradeEntry[]): Promise<Grade[]> {
    const response = await firstValueFrom(
      this.http.post<ApiResponse<Grade[]>>(`${BASE_URL}/exams/${examId}/grades`, { entries }),
    );
    return response.data;
  }
}
