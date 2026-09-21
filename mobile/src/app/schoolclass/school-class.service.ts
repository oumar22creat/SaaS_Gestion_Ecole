import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';
import { SchoolClass } from './school-class.model';

const BASE_URL = `${environment.apiUrl}/classes`;

/** Seul `list()` est nécessaire côté Mobile pour l'instant (feuille d'appel, saisie de notes) — la gestion des classes reste un usage desktop-first (voir docs/DESIGN.md §1), portée par web/src/app/schoolclass/school-class.service.ts. */
@Injectable({ providedIn: 'root' })
export class SchoolClassService {
  private readonly http = inject(HttpClient);

  async list(): Promise<SchoolClass[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<SchoolClass[]>>(BASE_URL, { params: { pageSize: 200 } }),
    );
    return response.data;
  }
}
