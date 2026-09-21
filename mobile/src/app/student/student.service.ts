import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';
import { Student } from './student.model';

const BASE_URL = `${environment.apiUrl}/students`;

@Injectable({ providedIn: 'root' })
export class StudentService {
  private readonly http = inject(HttpClient);

  async list(): Promise<Student[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<Student[]>>(BASE_URL, { params: { pageSize: 200 } }),
    );
    return response.data;
  }
}
