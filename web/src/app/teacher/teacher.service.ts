import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';
import { Teacher, TeacherRequest } from './teacher.model';

const BASE_URL = `${environment.apiUrl}/teachers`;

@Injectable({ providedIn: 'root' })
export class TeacherService {
  private readonly http = inject(HttpClient);

  async list(): Promise<Teacher[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<Teacher[]>>(BASE_URL, { params: { pageSize: 200 } }),
    );
    return response.data;
  }

  async create(request: TeacherRequest): Promise<Teacher> {
    const response = await firstValueFrom(this.http.post<ApiResponse<Teacher>>(BASE_URL, request));
    return response.data;
  }

  async update(id: number, request: TeacherRequest): Promise<Teacher> {
    const response = await firstValueFrom(
      this.http.put<ApiResponse<Teacher>>(`${BASE_URL}/${id}`, request),
    );
    return response.data;
  }

  async deactivate(id: number): Promise<void> {
    await firstValueFrom(this.http.delete<void>(`${BASE_URL}/${id}`));
  }
}
