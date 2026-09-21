import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';
import { Subject } from './subject.model';

@Injectable({ providedIn: 'root' })
export class SubjectService {
  private readonly http = inject(HttpClient);

  async list(): Promise<Subject[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<Subject[]>>(`${environment.apiUrl}/subjects`, { params: { pageSize: 200 } }),
    );
    return response.data;
  }
}
