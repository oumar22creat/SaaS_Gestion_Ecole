import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';
import { PARAMS_REFERENTIEL } from '../core/referentiel.params';
import { Teacher } from './teacher.model';

const BASE_URL = `${environment.apiUrl}/teachers`;

/**
 * Seul `list()` est nécessaire côté Mobile : l'emploi du temps nomme l'enseignant de chaque
 * créneau, car un identifiant ne dit rien à qui consulte sa semaine. La gestion du personnel
 * reste un usage desktop-first (docs/DESIGN.md §1), portée par web/src/app/teacher/teacher.service.ts.
 */
@Injectable({ providedIn: 'root' })
export class TeacherService {
  private readonly http = inject(HttpClient);

  async list(): Promise<Teacher[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<Teacher[]>>(BASE_URL, { params: PARAMS_REFERENTIEL }),
    );
    return response.data;
  }
}
