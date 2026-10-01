import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';
import { PARAMS_REFERENTIEL } from '../core/referentiel.params';
import { Exam } from './grade.model';

const BASE_URL = `${environment.apiUrl}/exams`;

@Injectable({ providedIn: 'root' })
export class ExamService {
  private readonly http = inject(HttpClient);

  async list(): Promise<Exam[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<Exam[]>>(BASE_URL, { params: PARAMS_REFERENTIEL }),
    );
    return response.data;
  }

  async getById(id: number): Promise<Exam> {
    const response = await firstValueFrom(this.http.get<ApiResponse<Exam>>(`${BASE_URL}/${id}`));
    return response.data;
  }
}
