import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';
import { Student, StudentImportResult, StudentRequest } from './student.model';

const BASE_URL = `${environment.apiUrl}/students`;

@Injectable({ providedIn: 'root' })
export class StudentService {
  private readonly http = inject(HttpClient);

  async list(): Promise<Student[]> {
    const response = await firstValueFrom(this.http.get<ApiResponse<Student[]>>(BASE_URL, { params: { pageSize: 200 } }));
    return response.data;
  }

  async create(request: StudentRequest): Promise<Student> {
    const response = await firstValueFrom(this.http.post<ApiResponse<Student>>(BASE_URL, request));
    return response.data;
  }

  async update(id: number, request: StudentRequest): Promise<Student> {
    const response = await firstValueFrom(this.http.put<ApiResponse<Student>>(`${BASE_URL}/${id}`, request));
    return response.data;
  }

  async deactivate(id: number): Promise<void> {
    await firstValueFrom(this.http.delete<void>(`${BASE_URL}/${id}`));
  }

  async importCsv(file: File): Promise<StudentImportResult> {
    const formData = new FormData();
    formData.append('file', file);
    const response = await firstValueFrom(
      this.http.post<ApiResponse<StudentImportResult>>(`${BASE_URL}/import`, formData),
    );
    return response.data;
  }
}
