import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { ApiResponse } from '../core/api-response.model';
import { Room } from './room.model';

const BASE_URL = `${environment.apiUrl}/rooms`;

/**
 * Seul `list()` est nécessaire côté Mobile : l'emploi du temps a besoin du nom de la salle,
 * la gestion des salles reste un usage desktop-first (docs/DESIGN.md §1), portée par
 * web/src/app/timetable/room.service.ts.
 */
@Injectable({ providedIn: 'root' })
export class RoomService {
  private readonly http = inject(HttpClient);

  async list(): Promise<Room[]> {
    const response = await firstValueFrom(
      this.http.get<ApiResponse<Room[]>>(BASE_URL, { params: { pageSize: 200 } }),
    );
    return response.data;
  }
}
