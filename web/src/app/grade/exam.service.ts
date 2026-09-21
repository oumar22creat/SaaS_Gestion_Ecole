import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';
import { Exam, ExamRequest, ExamStatistics } from './grade.model';

const BASE_URL = `${environment.apiUrl}/exams`;

@Injectable({ providedIn: 'root' })
export class ExamService {
  private readonly http = inject(HttpClient);

  async list(): Promise<Exam[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<Exam[]>>(BASE_URL, { params: { pageSize: 200 } }),
    );
    return response.data;
  }

  async getById(id: number): Promise<Exam> {
    const response = await firstValueFrom(this.http.get<ApiResponse<Exam>>(`${BASE_URL}/${id}`));
    return response.data;
  }

  async create(request: ExamRequest): Promise<Exam> {
    const response = await firstValueFrom(this.http.post<ApiResponse<Exam>>(BASE_URL, request));
    return response.data;
  }

  async remove(id: number): Promise<void> {
    await firstValueFrom(this.http.delete<void>(`${BASE_URL}/${id}`));
  }

  async statistics(examId: number): Promise<ExamStatistics> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<ExamStatistics>>(`${BASE_URL}/${examId}/statistics`),
    );
    return response.data;
  }
}
