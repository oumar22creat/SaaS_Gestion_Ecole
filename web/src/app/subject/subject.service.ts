import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';
import { Subject, SubjectRequest } from './subject.model';

const BASE_URL = `${environment.apiUrl}/subjects`;

@Injectable({ providedIn: 'root' })
export class SubjectService {
  private readonly http = inject(HttpClient);

  async list(): Promise<Subject[]> {
    const response = await firstValueFrom(this.http.get<ApiResponse<Subject[]>>(BASE_URL, { params: { pageSize: 200 } }));
    return response.data;
  }

  async create(request: SubjectRequest): Promise<Subject> {
    const response = await firstValueFrom(this.http.post<ApiResponse<Subject>>(BASE_URL, request));
    return response.data;
  }

  async update(id: number, request: SubjectRequest): Promise<Subject> {
    const response = await firstValueFrom(this.http.put<ApiResponse<Subject>>(`${BASE_URL}/${id}`, request));
    return response.data;
  }

  async remove(id: number): Promise<void> {
    await firstValueFrom(this.http.delete<void>(`${BASE_URL}/${id}`));
  }
}
