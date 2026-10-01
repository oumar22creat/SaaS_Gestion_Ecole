import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';
import { PARAMS_REFERENTIEL } from '../core/referentiel.params';
import { Subject } from './subject.model';

@Injectable({ providedIn: 'root' })
export class SubjectService {
  private readonly http = inject(HttpClient);

  async list(): Promise<Subject[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<Subject[]>>(`${environment.apiUrl}/subjects`, { params: PARAMS_REFERENTIEL }),
    );
    return response.data;
  }
}
